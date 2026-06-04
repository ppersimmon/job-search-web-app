package com.example.job_search.parser;

import com.example.job_search.entity.Category;
import com.example.job_search.entity.Company;
import com.example.job_search.entity.Vacancy;
import com.example.job_search.repository.CategoryRepository;
import com.example.job_search.repository.CompanyRepository;
import com.example.job_search.repository.VacancyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotaUaParserService {

    private final VacancyRepository vacancyRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public void parseDiverseVacancies() {
        Map<Integer, String> categoriesMap = getCategoriesMap();

        for (Map.Entry<Integer, String> entry : categoriesMap.entrySet()) {
            Integer rubricId = entry.getKey();
            String categoryName = entry.getValue();

            Category category = categoryRepository.findByName(categoryName)
                    .orElseGet(() -> {
                        Category newCat = new Category();
                        newCat.setName(categoryName);
                        return categoryRepository.save(newCat);
                    });

            log.info("Category parsing (Robota.ua): {}", categoryName);

            try {
                String graphqlPayload = "{\"operationName\":\"getPublishedVacanciesList\",\"variables\":{\"pagination\":{\"count\":20,\"page\":0},\"filter\":{\"keywords\":\"\",\"metroBranches\":[],\"additionalKeywords\":\"\",\"clusterKeywords\":[],\"location\":{\"longitude\":0,\"latitude\":0},\"salary\":0,\"districtIds\":[],\"microDistrictIds\":[],\"scheduleIds\":[],\"rubrics\":[{\"id\":\"" + rubricId + "\",\"subrubricIds\":[]}],\"showAgencies\":true,\"showOnlyNoCvApplyVacancies\":false,\"showOnlySpecialNeeds\":false,\"showOnlyWithoutExperience\":false,\"showOnlyNotViewed\":false,\"showWithoutSalary\":true,\"showMilitary\":true,\"isReservation\":false,\"isForVeterans\":false,\"isOfficeWithGenerator\":false,\"isOfficeWithShelter\":false,\"isMilitary\":false,\"gender\":null,\"branchIds\":[]},\"sort\":\"BY_BUSINESS_SCORE\"},\"query\":\"query getPublishedVacanciesList($filter: PublishedVacanciesFilterInput!, $pagination: PublishedVacanciesPaginationInput!, $sort: PublishedVacanciesSortType!) { publishedVacancies(filter: $filter, pagination: $pagination, sort: $sort) { items { id title description city { name } company { id name } salary { amount } schedules { id } badges { name } } } }\"}";
                String jsonResponse = Jsoup.connect("https://dracula.robota.ua/?q=getPublishedVacanciesList")
                        .method(Connection.Method.POST)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json, text/plain, */*")
                        .header("apollographql-client-name", "web-alliance-desktop")
                        .header("apollographql-client-version", "e6e4197")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .ignoreContentType(true)
                        .requestBody(graphqlPayload)
                        .execute()
                        .body();

                JsonNode root = objectMapper.readTree(jsonResponse);
                JsonNode items = root.path("data").path("publishedVacancies").path("items");

                if (items.isMissingNode() || !items.isArray() || items.isEmpty()) {
                    log.warn("No vacancies found for rubric {}.", rubricId);
                    continue;
                }

                for (JsonNode item : items) {
                    String id = item.path("id").asText();
                    String title = item.path("title").asText();
                    String companyId = item.path("company").path("id").asText();

                    String originalUrl = "https://robota.ua/company" + companyId + "/vacancy" + id;

                    var existingVacancyOpt = vacancyRepository.findByOriginalUrl(originalUrl);
                    if (existingVacancyOpt.isPresent()) {
                        Vacancy existing = existingVacancyOpt.get();
                        existing.setLastSeenAt(java.time.LocalDateTime.now());
                        vacancyRepository.save(existing);
                        continue;
                    }

                    String description = item.path("description").asText("");
                    String previewText = description.length() > 250 ? description.substring(0, 250) + "..." : description;

                    String companyNameStr = item.path("company").path("name").asText("Анонімна компанія");
                    int salaryAmount = item.path("salary").path("amount").asInt(0);
                    String salary = salaryAmount > 0 ? salaryAmount + " грн" : "Не вказано";

                    String city = item.path("city").path("name").asText("Не вказано");
                    boolean isRemote = false;

                    JsonNode schedules = item.path("schedules");
                    if (schedules.isArray()) {
                        for (JsonNode schedule : schedules) {
                            if ("3".equals(schedule.path("id").asText())) {
                                isRemote = true;
                                break;
                            }
                        }
                    }

                    if (!isRemote) {
                        JsonNode badges = item.path("badges");
                        if (badges.isArray()) {
                            for (JsonNode badge : badges) {
                                if ("Віддалена робота".equalsIgnoreCase(badge.path("name").asText())) {
                                    isRemote = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!isRemote && city.toLowerCase().contains("віддалено")) {
                        isRemote = true;
                    }

                    if (isRemote) {
                        city = "Дистанційно";
                    } else if (city.contains("(")) {
                        city = city.substring(0, city.indexOf("(")).trim();
                    }

                    Company company = companyRepository.findByName(companyNameStr)
                            .orElseGet(() -> {
                                Company newComp = new Company();
                                newComp.setName(companyNameStr);
                                return companyRepository.save(newComp);
                            });

                    Vacancy vacancy = new Vacancy();
                    vacancy.setTitle(title);
                    vacancy.setPreviewText(previewText);
                    vacancy.setOriginalUrl(originalUrl);
                    vacancy.setSalary(salary);
                    vacancy.setCity(city);
                    vacancy.setActive(true);
                    vacancy.setCompany(company);
                    vacancy.setCategory(category);

                    vacancyRepository.save(vacancy);
                }

                Thread.sleep(3000);

            } catch (IOException | InterruptedException e) {
                log.error("Parsing error for rubric {}: {}", rubricId, e.getMessage());
            }
        }
        log.info("Robota.ua parsing completed successfully!");
    }

    private Map<Integer, String> getCategoriesMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "IT");
        map.put(3, "Управління персоналом, HR");
        map.put(33, "Транспорт, автобізнес");
        map.put(11, "Секретаріат, діловодство, АГВ");
        map.put(18, "Фінанси, банк");
        map.put(6, "Бухгалтерія, аудит");
        map.put(14, "Бухгалтерія, аудит");
        map.put(8, "Готельно-ресторанний бізнес, туризм");
        map.put(23, "Готельно-ресторанний бізнес, туризм");
        map.put(15, "Дизайн, мистецтво");
        map.put(31, "Продаж, закупівля");
        map.put(17, "Продаж, закупівля");
        map.put(21, "Культура, музика, шоу-бізнес");
        map.put(5, "Логістика, склад, ЗЕД");
        map.put(24, "Маркетинг, реклама, PR");
        map.put(22, "ЗМІ, видавництво, поліграфія");
        map.put(9, "Медицина, фармацевтика");
        map.put(10, "Освіта, наука");
        map.put(28, "Нерухомість");
        map.put(4, "Охорона, безпека");
        map.put(32, "Робочі спеціальності, виробництво");
        map.put(20, "Робочі спеціальності, виробництво");
        map.put(26, "Сільське господарство, агробізнес");
        map.put(7, "Краса, фітнес, спорт");
        map.put(19, "Страхування");
        map.put(27, "Будівництво, архітектура");
        map.put(2, "Телекомунікації та зв'язок");
        map.put(12, "Топменеджмент, керівництво вищої ланки");
        map.put(16, "Роздрібна торгівля");
        map.put(29, "Юриспруденція");
        return map;
    }
}