# Skaner UFG Voice — wersja 0.1

To jest **projekt źródłowy Android**, a nie gotowy, podpisany APK.

## Co zawiera prototyp
- Otwiera portal UFG wewnątrz aplikacji (WebView).
- Udostępnia tryby zdjęcia i głosu. W trybie zdjęcia należy użyć funkcji zdjęcia dostępnej w formularzu UFG; WebView przekazuje wybór pliku do Androida.
- Rozpoznaje pojedynczą wypowiedź po polsku i obsługuje komendy: „Tryb zdjęcie”, „Tryb głosowy”, „Tak”, „Nie”, „Następny samochód”, „Powtórz”, „Zakończ”.
- Próbuje wstawić rozpoznany numer do pierwszego pola tekstowego strony. Należy sprawdzić, czy pole UFG nadal ma zgodną strukturę.
- Wypowiada komunikat „Potwierdź sprawdzenie”, gdy wykryje na stronie słowa sugerujące CAPTCHA.
- Zapisuje lokalną historię wyników oznaczonych przez użytkownika.

## Ważne ograniczenia prototypu
- Wynik OC jest oznaczany przyciskiem przez użytkownika; aplikacja nie odczytuje jeszcze wiarygodnie wyniku z portalu.
- Wykrywanie CAPTCHA jest orientacyjne i zależy od tekstu strony. Nie rozwiązuje ani nie omija CAPTCHA.
- Komendy głosowe są uruchamiane na żądanie (rozpoznawanie pojedynczej wypowiedzi), nie ma ciągłego nasłuchu w tle.
- Wybór zdjęcia zależy od mechanizmu udostępnianego przez aktualny formularz UFG i Android WebView.
- UFG może zmienić stronę, co może wymagać aktualizacji aplikacji.
- Aplikacja nie zapisuje zdjęć w galerii automatycznie; wykorzystuje zdjęcie wybierane/robione w formularzu UFG.
- Nie należy traktować komunikatu „brak potwierdzenia” jako pewności, że pojazd nie ma OC.

## Budowanie APK
1. Zainstaluj Android Studio na komputerze.
2. Otwórz folder `SkanerUFGVoice` jako projekt.
3. Poczekaj na synchronizację Gradle i instalację wymaganych Android SDK (compileSdk 35).
4. Wybierz **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK debug znajdziesz zwykle w `app/build/outputs/apk/debug/`.

## Wymagania
- Android Studio z JDK 17 lub nowszym.
- Android SDK Platform 35.
- Połączenie internetowe.
- Usługi rozpoznawania mowy w języku polskim dostępne na urządzeniu.
