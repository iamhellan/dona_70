package org.example;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class v2_promo {
    @Test
    void openAllBonusesInTabs() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setArgs(List.of("--start-maximized")));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(null));
            Page page = context.newPage();

            // 1. Заходим на сайт
            page.navigate("https://1xbet.kz/");
            System.out.println("Открыли https://1xbet.kz/");

            // 2. Кликаем на раздел "1XBONUS" в верхнем меню
            page.waitForSelector("a[href='bonus/rules']", new Page.WaitForSelectorOptions().setTimeout(10000));
            page.click("a[href='bonus/rules']");
            System.out.println("Перешли в раздел 1XBONUS");

            // 3. Кликаем по свитчу "Все бонусы"
            page.waitForSelector("span.bonuses-navigation-bar__caption:has-text('Все бонусы')", new Page.WaitForSelectorOptions().setTimeout(10000));
            page.click("span.bonuses-navigation-bar__caption:has-text('Все бонусы')");
            System.out.println("Нажали 'Все бонусы'");

            // 4. Ждём список бонусов
            page.waitForSelector("ul.bonuses-list", new Page.WaitForSelectorOptions().setTimeout(10000));
            List<ElementHandle> bonusLinks = page.querySelectorAll("ul.bonuses-list a.bonus-tile");
            if (bonusLinks.isEmpty()) {
                throw new RuntimeException("Не найдено ни одного бонуса!");
            }
            System.out.println("Нашли бонусов: " + bonusLinks.size());

            // 5. Открываем каждый бонус в новой вкладке
            List<Page> tabs = new ArrayList<>();
            for (int i = 0; i < bonusLinks.size(); i++) {
                String href = bonusLinks.get(i).getAttribute("href");
                String url = href.startsWith("http") ? href : "https://1xbet.kz" + href;
                Page newTab = context.newPage();
                newTab.navigate(url);
                tabs.add(newTab);
                System.out.println("Открыли бонус #" + (i + 1) + " в новой вкладке: " + url);
            }

            System.out.println("Готово: все бонусы открыты в новых вкладках.");
            // Спим чтобы можно было вручную посмотреть результат
            page.waitForTimeout(5000);
        }
    }
}
