# Домашнее задание к уроку Статический анализ: Android Lint и Detekt

## Задание #1

Корутины, запущенные в `kotlinx.coroutines.GlobalScope` нужно контролировать вне скоупа класса, в котором они созданы. Контролировать глобальные корутины неудобно, а отсутствие контроля может привести к излишнему использованию ресурсов и утечкам памяти.

Подробнее: [https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc](https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc)

### Что нужно сделать

1. Реализуйте detekt правило, которое найдет запуск корутин через `launch` или `async` в `GlobalScope`.
2. Напишите тесты на ваше правило.

## Задание #2

Частая ошибка при использовании корутин — запускать top level корутину внутри suspend функции, что приводит к нарушению structured concurrency.

Подробнее: [https://elizarov.medium.com/structured-concurrency-722d765aa952](https://elizarov.medium.com/structured-concurrency-722d765aa952)

### Что нужно сделать

1. Реализуйте detekt правило, используя type resolution, которое найдет запуск корутины через `launch` или `async` над любыми наследниками `kotlinx.coroutines.CoroutineScope` внутри suspend функции.
2. Правило не должно репортить об использовании scope-билдеров внутри suspend функции (`coroutineScope`, `supervisorScope`)
3. Напишите тесты на ваше правило.
