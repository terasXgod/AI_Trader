# AI Trader Dataset Creator

Полноценный парсер для сбора временных рядов криптовалют из двух источников:

- Coinlore (реалтайм polling)
- Yahoo Finance (исторические свечи)

Скрипт находится в `datasetCreator.py`.

## Что делает парсер

1. Работает с источником `coinlore` или `yahoo`.
2. Для Coinlore собирает точки с заданным интервалом.
3. Для Yahoo импортирует исторические OHLCV свечи.
4. Сохраняет датасет в CSV.
5. Опционально формирует supervised-датасет с лагами (`lag_1 ... lag_N`) и целевой переменной (`target_price`, `target_return`).

## Источники данных

### 1) Coinlore (`--source coinlore`)

- Endpoint: `https://api.coinlore.net/api/ticker`
- Дает только текущее состояние монеты (не исторические свечи)
- История формируется локально за счет регулярного опроса

### 2) Yahoo Finance (`--source yahoo`)

- Endpoint: Yahoo chart API
- Дает исторические свечи (OHLCV)
- Можно выбрать `symbol`, `interval`, `range` или период дат `start/end`
- Это режим для импорта истории "задним числом"

## Важный момент про временные ряды и размер свечи

Да, вы получаете временные ряды.

- Базовый ряд (`coinlore_timeseries.csv`) состоит из точек, собранных с шагом `--interval-sec`.
- API `ticker` Coinlore не отдает готовые исторические свечи с выбором таймфрейма.
- Поэтому размер свечи выбирается локально: скрипт строит OHLC из собранных точек с параметром `--candle-sec`.

Для `--source yahoo` размер свечи задается напрямую параметром `--yahoo-interval`, потому что свечи приходят уже готовыми из API.

Итог:
- шаг базового ряда = `--interval-sec`
- размер свечи = `--candle-sec` (локальная агрегация)

## Требования

- Python 3.9+
- Интернет-доступ к API Coinlore
- Дополнительные библиотеки не требуются (используется только стандартная библиотека Python)

## Быстрый старт

### 1. Проверка Python

```bash
python --version
```

### 2. Быстрый сбор тестового набора

```bash
python datasetCreator.py --source coinlore --coin-id 90 --interval-sec 5 --samples 10
```

Результат:
- `data/coinlore_timeseries.csv`
- `data/coinlore_candles.csv`
- `data/coinlore_windows.csv`

Быстрый импорт истории из Yahoo:

```bash
python datasetCreator.py --source yahoo --yahoo-symbol BTC-USD --yahoo-interval 1h --yahoo-range 1mo --output data/yahoo_btc_1h.csv
```

## Аргументы CLI

```bash
python datasetCreator.py [OPTIONS]
```

- `--coin-id` (int, по умолчанию `90`): ID монеты в Coinlore (например, BTC).
- `--interval-sec` (int, по умолчанию `60`): интервал опроса API в секундах.
- `--samples` (int, по умолчанию `120`): сколько точек собрать за запуск.
- `--yahoo-symbol` (str, по умолчанию `BTC-USD`): тикер Yahoo (например, `ETH-USD`).
- `--yahoo-interval` (str, по умолчанию `1h`): интервал свечи Yahoo (например, `1m`, `5m`, `1h`, `1d`).
- `--yahoo-range` (str, по умолчанию `1mo`): диапазон истории Yahoo (`7d`, `1mo`, `6mo`, `1y`, `max`).
- `--start-date` (str, опционально): начальная дата в формате `YYYY-MM-DD`.
- `--end-date` (str, опционально): конечная дата в формате `YYYY-MM-DD`.
- `--output` (path, по умолчанию `data/coinlore_timeseries.csv`): путь к CSV с сырыми данными.
- `--candles-output` (path, по умолчанию `data/coinlore_candles.csv`): путь к CSV со свечами OHLC.
- `--candle-sec` (int, по умолчанию `60`): таймфрейм свечи в секундах.
- `--windows-output` (path, по умолчанию `data/coinlore_windows.csv`): путь к CSV с окнами для обучения.
- `--lookback` (int, по умолчанию `30`): размер окна истории (количество лагов).
- `--horizon` (int, по умолчанию `1`): горизонт прогноза в шагах.
- `--timeout` (int, по умолчанию `15`): timeout HTTP-запроса в секундах.
- `--retries` (int, по умолчанию `3`): число повторных попыток при сбоях API.

### Примеры

Coinlore: сбор 300 точек по BTC с интервалом 1 минута:

```bash
python datasetCreator.py --source coinlore --coin-id 90 --interval-sec 60 --samples 300
```

Coinlore: сбор с более длинным окном истории и горизонтом:

```bash
python datasetCreator.py --source coinlore --coin-id 90 --interval-sec 60 --samples 500 --lookback 60 --horizon 5
```

Сбор точек раз в 10 секунд и свечи 1 минута:

```bash
python datasetCreator.py --source coinlore --coin-id 90 --interval-sec 10 --samples 360 --candle-sec 60
```

Сбор точек раз в 5 секунд и свечи 5 минут:

```bash
python datasetCreator.py --source coinlore --coin-id 90 --interval-sec 5 --samples 720 --candle-sec 300
```

Отключить генерацию supervised-окон:

```bash
python datasetCreator.py --source coinlore --coin-id 90 --interval-sec 60 --samples 200 --windows-output ""
```

Отключить генерацию свечей:

```bash
python datasetCreator.py --source coinlore --coin-id 90 --interval-sec 60 --samples 200 --candles-output "" --candle-sec 0
```

Yahoo: история BTC-USD, свечи 1 час, диапазон 6 месяцев:

```bash
python datasetCreator.py --source yahoo --yahoo-symbol BTC-USD --yahoo-interval 1h --yahoo-range 6mo --output data/yahoo_btc_1h_6mo.csv
```

Yahoo: история ETH-USD по датам:

```bash
python datasetCreator.py --source yahoo --yahoo-symbol ETH-USD --yahoo-interval 1d --start-date 2024-01-01 --end-date 2025-01-01 --output data/yahoo_eth_1d_2024.csv
```

## Формат сырых данных (timeseries)

Файл: `data/coinlore_timeseries.csv`

Колонки:
- `fetched_at_utc`: время запроса (UTC, ISO-8601)
- `coin_id`: ID монеты
- `symbol`: тикер (например, BTC)
- `name`: имя монеты
- `rank`: место по капитализации
- `price_usd`: цена в USD
- `price_btc`: цена в BTC
- `volume24`: объём за 24 часа
- `market_cap_usd`: капитализация
- `percent_change_1h`: изменение за 1 час
- `percent_change_24h`: изменение за 24 часа
- `percent_change_7d`: изменение за 7 дней
- `ts_api`: timestamp из API Coinlore
- `source`: источник (`coinlore_ticker`)

## Формат обучающих окон (supervised)

Файл: `data/coinlore_windows.csv`

Колонки:
- `window_start`: время первой точки в окне истории
- `window_end`: время последней точки истории
- `target_time`: время целевого значения
- `lag_1 ... lag_N`: исторические цены (`N = lookback`)
- `target_price`: цена в момент `target_time`
- `target_return`: относительная доходность относительно последнего лага

Формула target_return:

```text
target_return = (target_price - last_hist_price) / last_hist_price
```

Количество возможных окон для набора длины `L`:

```text
windows_count = L - lookback - horizon + 1
```

Если значение `windows_count <= 0`, файл окон будет пустым.

## Формат свечей (OHLC)

Файл: `data/coinlore_candles.csv`

Колонки:
- `coin_id`: ID монеты
- `interval_sec`: размер свечи в секундах
- `candle_start_utc`: начало свечи (UTC)
- `candle_end_utc`: конец свечи (UTC)
- `open`: первая цена в интервале
- `high`: максимальная цена в интервале
- `low`: минимальная цена в интервале
- `close`: последняя цена в интервале
- `tick_count`: сколько точек попало в свечу

Важно:
- это локально построенные свечи из собранных тиков, а не готовые свечи от API
- для более точных свечей уменьшайте `--interval-sec`
- желательно выбирать `--candle-sec >= --interval-sec`

## Формат истории Yahoo (OHLCV)

Файл: путь из `--output` в режиме `--source yahoo`

Колонки:
- `timestamp_utc`: время свечи (UTC)
- `symbol`: тикер Yahoo (например, `BTC-USD`)
- `interval`: выбранный интервал (`--yahoo-interval`)
- `open`, `high`, `low`, `close`: значения OHLC
- `volume`: объем
- `source`: `yahoo_finance_chart`

## Как выбрать параметры для обучения

- Для коротких стратегий:
  - `interval-sec`: 10-60 секунд
  - `lookback`: 30-120
  - `horizon`: 1-5
- Для более сглаженного сигнала:
  - увеличьте `interval-sec`
  - увеличьте `lookback`
- Минимальный объём данных:
  - желательно собирать хотя бы несколько тысяч точек для первых экспериментов

## Устойчивость к ошибкам

Скрипт поддерживает:
- повторные попытки (`--retries`) с экспоненциальной паузой
- HTTP timeout (`--timeout`)
- корректное завершение по Ctrl+C

## Ограничения

- Этот скрипт не собирает исторические свечи, а только текущий тикер через регулярный polling.
- Данные зависят от стабильности сети и доступности API.
- При большом `samples` и малом `interval-sec` запуск может занимать длительное время.

Дополнительно для Yahoo:
- Некоторые сочетания `interval` и глубины истории ограничены на стороне Yahoo.
- Если нужны длинные периоды, используйте более крупный интервал (`1h`, `1d`) или диапазон `max`.

## Рекомендованный рабочий процесс

1. Запустить сбор сырых данных.
2. Проверить качество и непрерывность ряда.
3. Сформировать supervised-окна.
4. Разделить датасет на train/validation/test.
5. Обучить модель и оценить метрики.

## Структура проекта

```text
AI_Trader/
  datasetCreator.py
  README.md
  data/
    coinlore_timeseries.csv
    coinlore_candles.csv
    coinlore_windows.csv
    yahoo_btc_1h.csv
```

## Полезные команды

Запуск с логированием в файл:

```bash
python datasetCreator.py --source coinlore --coin-id 90 --interval-sec 60 --samples 300 > run.log 2>&1
```

Запуск для другой монеты (подставьте нужный ID):

```bash
python datasetCreator.py --source coinlore --coin-id 80 --interval-sec 60 --samples 300
```

Импорт истории из Yahoo в отдельный файл:

```bash
python datasetCreator.py --source yahoo --yahoo-symbol BTC-USD --yahoo-interval 4h --yahoo-range 1y --output data/yahoo_btc_4h_1y.csv
```

---

Если нужно, можно расширить скрипт до:
- сбора сразу нескольких монет в один датасет
- бесконечного режима с остановкой по Ctrl+C
- автоматического экспорта train/val/test в отдельные CSV
