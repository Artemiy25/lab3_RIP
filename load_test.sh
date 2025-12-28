#!/bin/bash

set -euo pipefail

# Цвета для вывода
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly RED='\033[0;31m'
readonly NC='\033[0m' # No Color

# Параметры
readonly SERVICE_URL="${SERVICE_URL:-http://localhost:8081}"
readonly ENDPOINT="/api/random/statistics"
readonly LOG_FILE="load_test_results.log"
readonly MAX_TIMEOUT=300

# Проверка необходимых команд
command -v curl >/dev/null 2>&1 || { echo "ERROR: curl не установлен" >&2; exit 1; }
command -v date >/dev/null 2>&1 || { echo "ERROR: date не установлен" >&2; exit 1; }

echo "=========================================="
echo "Load Testing для Andesis Service B"
echo "=========================================="
echo "URL: ${SERVICE_URL}${ENDPOINT}"
echo "Начало теста: $(date)"
echo ""

# Очистка лог файла
: > "${LOG_FILE}"

# Функция для безопасного выполнения curl запроса
safe_curl_test() {
    local count="$1"
    local test_num="$2"
    local label="$3"

    local start end duration http_code curl_time

    start=$(date +%s%3N)
    local response
    response=$(curl --silent --show-error --fail-with-body \
        --max-time "${MAX_TIMEOUT}" \
        --write-out "\nHTTP_CODE:%{http_code}\nTIME_TOTAL:%{time_total}" \
        "${SERVICE_URL}${ENDPOINT}?count=${count}&min=0&max=1000000" 2>&1) || true
    end=$(date +%s%3N)
    duration=$((end - start))

    http_code=$(echo "${response}" | grep "HTTP_CODE:" | cut -d':' -f2 || echo "000")
    curl_time=$(echo "${response}" | grep "TIME_TOTAL:" | cut -d':' -f2 || echo "N/A")

    if [ "${http_code}" = "200" ]; then
        printf "${GREEN}[OK]${NC} Запрос #%s (%s): %ss (%sms)\n" "${test_num}" "${label}" "${curl_time}" "${duration}" | tee -a "${LOG_FILE}"
    else
        printf "${RED}[FAIL]${NC} Запрос #%s (%s): HTTP %s\n" "${test_num}" "${label}" "${http_code}" | tee -a "${LOG_FILE}"
    fi
}

echo "Тест 1: Параллельные запросы на 1M чисел (5 запросов)"
echo "------------------------------------------"
for i in {1..5}; do
    {
        safe_curl_test 1000000 "${i}" "1M"
    } &
done
wait
echo ""

sleep 2

echo "Тест 2: Параллельные запросы на 5M чисел (3 запроса)"
echo "------------------------------------------"
for i in {1..3}; do
    {
        safe_curl_test 5000000 "${i}" "5M"
    } &
done
wait
echo ""

sleep 2

echo "Тест 3: Запрос на 10M чисел (1 запрос)"
echo "------------------------------------------"
safe_curl_test 10000000 "1" "10M"
echo ""

sleep 2

echo "Тест 4: Смешанная нагрузка (параллельно: 2x1M + 1x5M + 1x10M)"
echo "------------------------------------------"

# 2x1M
for i in {1..2}; do
    {
        safe_curl_test 1000000 "${i}" "Mixed-1M"
    } &
done

# 1x5M
{
    safe_curl_test 5000000 "1" "Mixed-5M"
} &

# 1x10M
{
    safe_curl_test 10000000 "1" "Mixed-10M"
} &

wait
echo ""

echo "=========================================="
echo "Тест завершен: $(date)"
echo "Результаты сохранены в ${LOG_FILE}"
echo "=========================================="
echo ""
echo "Для просмотра JFR файлов:"
echo "  ls -lh jfr-logs/"
echo ""
printf "${YELLOW}ВАЖНО:${NC} JFR профилирование длится 180 секунд.\n"
echo "       Подождите завершения записи перед анализом файла."
