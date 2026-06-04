package com.example.job_search.parser;
import org.springframework.scheduling.annotation.Scheduled;
import com.example.job_search.entity.Category;
import com.example.job_search.entity.Company;
import com.example.job_search.entity.Vacancy;
import com.example.job_search.repository.CategoryRepository;
import com.example.job_search.repository.CompanyRepository;
import com.example.job_search.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkUaParserService {

    private final VacancyRepository vacancyRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;

    @Scheduled(cron = "0 0 9 * * *")
    public void parseDiverseVacancies() {
        Map<String, String> categoriesMap = getCategoriesMap();

        int pagesPerCategory = 1;

        for (Map.Entry<String, String> entry : categoriesMap.entrySet()) {
            String categoryUrl = entry.getKey();
            String categoryName = entry.getValue();

            Category category = categoryRepository.findByName(categoryName)
                    .orElseGet(() -> {
                        Category newCat = new Category();
                        newCat.setName(categoryName);
                        return categoryRepository.save(newCat);
                    });

            log.info("Category parsing: {}", categoryName);

            for (int i = 1; i <= pagesPerCategory; i++) {
                String urlWithPage = categoryUrl + "?page=" + i;

                try {
                    Document doc = Jsoup.connect(urlWithPage)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .referrer("https://www.google.com/")
                            .header("Accept-Language", "uk-UA,uk;q=0.9,en-US;q=0.8,en;q=0.7")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                            .header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                            .header("Sec-Ch-Ua-Mobile", "?0")
                            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                            .header("Sec-Fetch-Dest", "document")
                            .header("Sec-Fetch-Mode", "navigate")
                            .header("Sec-Fetch-Site", "cross-site")
                            .header("Sec-Fetch-User", "?1")
                            .header("Upgrade-Insecure-Requests", "1")
                            .timeout(10000)
                            .get();

                    Elements vacancyCards = doc.select(".card-hover");

                    if (vacancyCards.isEmpty()) {
                        log.warn("There are no vacancies on the page {}. Moving to the next.", urlWithPage);
                        break;
                    }

                    for (Element card : vacancyCards) {
                        Element titleElement = card.selectFirst("h2 a");
                        if (titleElement == null) continue;

                        String originalUrl = "https://www.work.ua" + titleElement.attr("href");

                        var existingVacancyOpt = vacancyRepository.findByOriginalUrl(originalUrl);
                        if (existingVacancyOpt.isPresent()) {
                            Vacancy existing = existingVacancyOpt.get();
                            existing.setLastSeenAt(java.time.LocalDateTime.now());
                            vacancyRepository.save(existing);
                            continue;
                        }

                        String title = titleElement.text();

                        String salary = "Не вказано";
                        Elements textElements = card.select("div, span, b, strong");
                        for (Element el : textElements) {
                            String text = el.text();
                            if (text.contains("грн") && text.matches(".*\\d.*") && text.length() < 40) {
                                salary = text.replace("Вища за середню", "").trim();
                                break;
                            }
                        }

                        String companyNameStr = "Анонімна компанія";
                        Element companyLogo = card.selectFirst("img[alt]");
                        if (companyLogo != null && !companyLogo.attr("alt").isEmpty() && !companyLogo.attr("alt").contains("Work.ua")) {
                            companyNameStr = companyLogo.attr("alt");
                        } else {
                            Element companyLink = card.selectFirst("a[href*=/by-company/]");
                            if (companyLink != null && !companyLink.text().isEmpty()) {
                                companyNameStr = companyLink.text();
                            } else {
                                Elements boldTags = card.select("b, strong, .strong-500, .strong-600");
                                for (Element el : boldTags) {
                                    String t = el.text();
                                    if (!t.contains("грн") && !t.toLowerCase().contains("вища") && !t.toLowerCase().contains("гаряча") && t.length() > 2) {
                                        companyNameStr = t;
                                        break;
                                    }
                                }
                            }
                        }

                        String city = "Не вказано";
                        if (card.text().contains("Дистанційно") || card.text().contains("Віддалена робота")) {
                            city = "Дистанційно";
                        } else {
                            Element mtXsDiv = card.selectFirst(".mt-xs");
                            if (mtXsDiv != null) {
                                Elements spans = mtXsDiv.select("> span");
                                for (Element span : spans) {
                                    String cls = span.className();
                                    if (!cls.contains("mr-xs") && !cls.contains("distance-block")) {
                                        String possibleCity = span.text().replace(",", "").replace("·", "").trim();
                                        if (!possibleCity.matches(".*\\d.*") && possibleCity.length() > 2) {
                                            city = possibleCity;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        Element descriptionElement = card.selectFirst("p");
                        String previewText = descriptionElement != null ? descriptionElement.text() : "";
                        if (previewText.length() > 250) previewText = previewText.substring(0, 250) + "...";

                        final String finalCompanyName = companyNameStr;
                        Company company = companyRepository.findByName(finalCompanyName)
                                .orElseGet(() -> {
                                    Company newComp = new Company();
                                    newComp.setName(finalCompanyName);
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

                    Thread.sleep(5000);

                } catch (IOException | InterruptedException e) {
                    log.error("Parsing error: {}", e.getMessage());
                }
            }
        }
        log.info("Parsing completed successfully!");
    }

    private Map<String, String> getCategoriesMap() {
        Map<String, String> map = new HashMap<>();
        map.put("https://www.work.ua/jobs-it/", "IT");
        map.put("https://www.work.ua/jobs-administration/", "Адміністрація, керівництво середньої ланки");
        map.put("https://www.work.ua/jobs-accounting/", "Бухгалтерія, аудит");
        map.put("https://www.work.ua/jobs-hotel-restaurant-tourism/", "Готельно-ресторанний бізнес, туризм");
        map.put("https://www.work.ua/jobs-design-art/", "Дизайн, мистецтво");
        map.put("https://www.work.ua/jobs-beauty-sports/", "Краса, фітнес, спорт");
        map.put("https://www.work.ua/jobs-culture-music-showbiz/", "Культура, музика, шоу-бізнес");
        map.put("https://www.work.ua/jobs-logistic-supply-chain/", "Логістика, склад, ЗЕД");
        map.put("https://www.work.ua/jobs-marketing-advertising-pr/", "Маркетинг, реклама, PR");
        map.put("https://www.work.ua/jobs-healthcare/", "Медицина, фармацевтика");
        map.put("https://www.work.ua/jobs-real-estate/", "Нерухомість");
        map.put("https://www.work.ua/jobs-education-scientific/", "Освіта, наука");
        map.put("https://www.work.ua/jobs-security/", "Охорона, безпека");
        map.put("https://www.work.ua/jobs-sales/", "Продаж, закупівля");
        map.put("https://www.work.ua/jobs-production-engineering/", "Робочі спеціальності, виробництво");
        map.put("https://www.work.ua/jobs-retail/", "Роздрібна торгівля");
        map.put("https://www.work.ua/jobs-office-secretarial/", "Секретаріат, діловодство, АГВ");
        map.put("https://www.work.ua/jobs-agriculture/", "Сільське господарство, агробізнес");
        map.put("https://www.work.ua/jobs-publishing-media/", "ЗМІ, видавництво, поліграфія");
        map.put("https://www.work.ua/jobs-insurance/", "Страхування");
        map.put("https://www.work.ua/jobs-construction-architecture/", "Будівництво, архітектура");
        map.put("https://www.work.ua/jobs-customer-service/", "Сфера обслуговування");
        map.put("https://www.work.ua/jobs-telecommunications/", "Телекомунікації та зв'язок");
        map.put("https://www.work.ua/jobs-management-executive/", "Топменеджмент, керівництво вищої ланки");
        map.put("https://www.work.ua/jobs-auto-transport/", "Транспорт, автобізнес");
        map.put("https://www.work.ua/jobs-hr-recruitment/", "Управління персоналом, HR");
        map.put("https://www.work.ua/jobs-banking-finance/", "Фінанси, банк");
        map.put("https://www.work.ua/jobs-legal/", "Юриспруденція");
        return map;
    }
}