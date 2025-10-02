package org.example;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class v2_social_authorization {
    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    static Page page;

    @BeforeAll
    static void setUpAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(List.of("--start-maximized"))
        );
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(null));
        page = context.newPage();
    }

    @AfterAll
    static void tearDownAll() {
        System.out.println("Тест завершён ✅ (браузер остаётся открытым)");
    }

    @Test
    void loginWithGoogleAndSms() {
        System.out.println("Открываем сайт 1xbet.kz");
        page.navigate("https://1xbet.kz/");

        System.out.println("Жмём 'Войти'");
        page.click("button#login-form-call");

        System.out.println("Жмём кнопку Google");
        page.click("a.auth-social__link--google");

        // ждём попап Google
        Page popup = page.waitForPopup(() -> System.out.println("Ожидание окна Google..."));
        popup.waitForLoadState();

        System.out.println("Вводим email");
        popup.fill("input[type='email']", "mynameisjante@gmail.com");
        popup.click("button:has-text('Далее')");
        popup.waitForTimeout(2000);

        System.out.println("Вводим пароль");
        popup.fill("input[type='password']", "Hesoyam11+");
        popup.click("button:has-text('Далее')");
        popup.waitForClose(() -> {});

        // ---- ЖМЁМ ВЫСЛАТЬ КОД ----
        System.out.println("Жмём 'Выслать код'");
        Locator sendCodeButton = page.locator("button:has-text('Выслать код')");
        sendCodeButton.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        try {
            sendCodeButton.click();
            System.out.println("Кнопка 'Выслать код' нажата ✅");
        } catch (Exception e) {
            System.out.println("Первая попытка клика не удалась, пробуем через JS...");
            page.evaluate("document.querySelector(\"button:has-text('Выслать код')\")?.click()");
        }

        // ---- ЖДЁМ РЕШЕНИЯ КАПЧИ (ПОЯВЛЕНИЕ ПОЛЯ) ----
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

        // ---- ОТКРЫВАЕМ GOOGLE MESSAGES, ВЫТАСКИВАЕМ КОД ----
        Page messagesPage = context.newPage();
        messagesPage.navigate("https://messages.google.com/web/conversations");

        System.out.println("Закрываем уведомление 'Нет, не нужно' (если есть)");
        if (messagesPage.locator("button:has-text('Нет, не нужно')").isVisible()) {
            messagesPage.waitForTimeout(1000);
            messagesPage.click("button:has-text('Нет, не нужно')");
        }

        System.out.println("Жмём 'Подключить, отсканировав QR-код'");
        messagesPage.waitForTimeout(1000);
        messagesPage.locator("span.qr-text:has-text('Подключить, отсканировав QR-код')").click();

        System.out.println("Ищем последнее сообщение от 1xBet");
        Locator lastMessage = messagesPage.locator("mws-conversation-list-item").first();
        lastMessage.waitFor();

        String smsText = lastMessage.innerText();
        System.out.println("Содержимое SMS: " + smsText);

        Pattern pattern = Pattern.compile("\\b[a-zA-Z0-9]{6,8}\\b");
        Matcher matcher = pattern.matcher(smsText);
        String code = null;
        if (matcher.find()) {
            code = matcher.group();
        }

        if (code == null) {
            throw new RuntimeException("Не удалось извлечь код подтверждения из SMS: " + smsText);
        }

        System.out.println("Извлечённый код подтверждения: " + code);

        // ---- ВОЗВРАЩАЕМСЯ И ВВОДИМ КОД ----
        page.bringToFront();

        System.out.println("Вводим код подтверждения");
        page.fill("input.phone-sms-modal-code__input", code);

        System.out.println("Жмём 'Подтвердить'");
        page.waitForTimeout(1000);
        page.click("button:has-text('Подтвердить')");

        System.out.println("Авторизация завершена ✅");

        // --- КЛИК ПО КРЕСТИКУ (если есть) ---
        System.out.println("Пробуем закрыть popup по крестику (если он вообще есть)");
        try {
            Locator closeCross = page.locator("div.box-modal_close.arcticmodal-close");
            closeCross.waitFor(new Locator.WaitForOptions().setTimeout(2000).setState(WaitForSelectorState.ATTACHED));
            if (closeCross.isVisible()) {
                closeCross.click();
                System.out.println("Крестик найден и нажат ✅");
            } else {
                System.out.println("Крестика нет — идём дальше");
            }
        } catch (Exception e) {
            System.out.println("Всплывашки или крестика нет, игнорируем и двигаемся дальше");
        }

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
