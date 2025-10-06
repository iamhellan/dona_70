package org.example;

import com.microsoft.playwright.*;
import java.nio.file.Paths;

public class OneXBET {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(false)
            );
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            // Открываем Google Messages — жди, пока сам вручную залогинишься (сканируешь QR)
            page.navigate("https://1xbet.kz");
            System.out.println("Авторизуйся в 1xbet и нажми Enter в консоли для продолжения...");
            System.in.read();

            // Сохраняем storageState после авторизации
            context.storageState(new BrowserContext.StorageStateOptions()
                    .setPath(Paths.get("messages-session.json")));
            System.out.println("Сессия сохранена в messages-session.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
