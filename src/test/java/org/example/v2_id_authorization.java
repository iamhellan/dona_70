package org.example;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;

public class v2_id_authorization {
    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    static Page page;

    @BeforeAll
    static void setUpAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        context = browser.newContext();
        page = context.newPage();
    }

    // Браузер остаётся открытым
    @AfterAll
    static void tearDownAll() {
        System.out.println("Тест завершён ✅ (браузер остаётся открытым)");
    }

    @Test
    void loginWithSms() {
        System.out.println("Открываем сайт 1xbet.kz");
        page.navigate("https://1xbet.kz/");

        System.out.println("Жмём 'Войти' в шапке");
        page.waitForTimeout(1000);
        page.click("button#login-form-call");

        System.out.println("Вводим ID");
        page.waitForTimeout(1000);
        page.fill("input#auth_id_email", "168715375");

        System.out.println("Вводим пароль");
        page.waitForTimeout(1000);
        page.fill("input#auth-form-password", "Aezakmi11+");

        System.out.println("Жмём 'Войти' в форме авторизации");
        page.waitForTimeout(1000);
        page.locator("button.auth-button:has-text('Войти')").click();

        System.out.println("Ждём 20 секунд (капча после 'Войти', если есть)");
        page.waitForTimeout(20000);

        System.out.println("Ждём модальное окно SMS");
        page.waitForSelector("button:has-text('Выслать код')");

        // ---- ЖМЁМ "ВЫСЛАТЬ КОД" ----
        System.out.println("Жмём 'Выслать код'");
        Locator sendCodeButton = page.locator("button:has-text('Выслать код')");
        try {
            sendCodeButton.click();
            System.out.println("Кнопка 'Выслать код' нажата ✅");
        } catch (Exception e) {
            System.out.println("Первая попытка клика не удалась, пробуем через JS...");
            page.evaluate("document.querySelector(\"button:has-text('Выслать код')\")?.click()");
        }

        // ---- ЖДЁМ РЕШЕНИЯ КАПЧИ ПОСЛЕ 'ВЫСЛАТЬ КОД' ----
        System.out.println("Теперь решай капчу вручную — я жду поле для кода (до 10 минут)...");
        try {
            page.waitForSelector("input.phone-sms-modal-code__input",
                    new Page.WaitForSelectorOptions()
                            .setTimeout(600_000)
                            .setState(WaitForSelectorState.VISIBLE)
            );
            System.out.println("Поле для кода появилось! Достаём код из Google Messages...");
        } catch (PlaywrightException e) {
            throw new RuntimeException("Поле для ввода кода не появилось — капча не решена или что-то пошло не так!");
        }

        System.out.println("Открываем Google Messages");
        Page messagesPage = context.newPage();
        messagesPage.navigate("https://messages.google.com/web/conversations");

        System.out.println("Закрываем уведомление 'Нет, не нужно' (если есть)");
        if (messagesPage.locator("button:has-text('Нет, не нужно')").isVisible()) {
            messagesPage.click("button:has-text('Нет, не нужно')");
        }

        System.out.println("Жмём кнопку 'Подключить, отсканировав QR-код'");
        messagesPage.locator("span.qr-text:has-text('Подключить, отсканировав QR-код')").click();

        System.out.println("Ищем последнее сообщение от 1xBet");
        Locator lastMessage = messagesPage.locator("mws-conversation-list-item").first();
        lastMessage.waitFor();

        String smsText = lastMessage.innerText();
        System.out.println("Содержимое SMS: " + smsText);

        String code = smsText.split("\\s+")[0].trim();
        System.out.println("Извлечённый код подтверждения: " + code);

        System.out.println("Возвращаемся на сайт 1xbet.kz");
        page.bringToFront();

        System.out.println("Вводим код подтверждения");
        page.fill("input.phone-sms-modal-code__input", code);

        System.out.println("Жмём 'Подтвердить'");
        page.click("button:has-text('Подтвердить')");

        System.out.println("Авторизация завершена ✅");

        // --- ЛИЧНЫЙ КАБИНЕТ ---
        System.out.println("Открываем 'Личный кабинет'");
        page.waitForTimeout(1000);
        page.click("a.header-lk-box-link[title='Личный кабинет']");

        // --- КЛИК ПО КРЕСТИКУ ЕЩЁ РАЗ (если всплыл modal в кабинете) ---
        System.out.println("Пробуем закрыть popup-крестик после входа в ЛК (если он вообще есть)");
        try {
            Locator closeCrossLk = page.locator("div.box-modal_close.arcticmodal-close");
            closeCrossLk.waitFor(new Locator.WaitForOptions().setTimeout(2000).setState(WaitForSelectorState.ATTACHED));
            if (closeCrossLk.isVisible()) {
                closeCrossLk.click();
                System.out.println("Крестик в ЛК найден и нажат ✅");
            } else {
                System.out.println("Крестика в ЛК нет — идём дальше");
            }
        } catch (Exception e) {
            System.out.println("Всплывашки в ЛК или крестика нет, игнорируем и двигаемся дальше");
        }

        // --- ВЫХОД ---
        System.out.println("Жмём 'Выход'");
        page.waitForTimeout(1000);
        page.click("a.ap-left-nav__item_exit");

        System.out.println("Подтверждаем выход кнопкой 'ОК'");
        page.waitForTimeout(1000);
        page.click("button.swal2-confirm.swal2-styled");

        System.out.println("Выход завершён ✅ (браузер остаётся открытым)");
    }
}
