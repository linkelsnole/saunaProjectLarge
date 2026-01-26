#!/bin/bash

SERVER_USER="webpanel"
SERVER_IP="212.164.86.237"
JAR_NAME="sauna_large.jar"
LOCAL_JAR="backend/target/sauna_large.jar"
SSH_KEY="$HOME/.ssh/deploy_key"


RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}🚀 Начинаем деплой...${NC}"


echo -e "${YELLOW}📦 Сборка проекта...${NC}"
cd backend
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Ошибка сборки Maven${NC}"
    exit 1
fi
cd ..
echo -e "${GREEN}✅ Сборка завершена${NC}"


echo -e "${YELLOW}🛑 Останавливаем старый процесс...${NC}"
ssh -i "$SSH_KEY" "${SERVER_USER}@${SERVER_IP}" 'bash -s' << 'ENDSSH'
    PORT_PID=$(lsof -ti:8085)
    if [ -n "$PORT_PID" ]; then
        echo "Найден процесс на порту 8085 (PID: $PORT_PID)"
        kill $PORT_PID 2>/dev/null
        sleep 2
        PORT_PID=$(lsof -ti:8085)
        if [ -n "$PORT_PID" ]; then
            echo "Принудительное завершение..."
            kill -9 $PORT_PID 2>/dev/null
            sleep 2
        fi
    fi
    
    pkill -f "sauna_large.jar" 2>/dev/null
    sleep 1
    
    if lsof -ti:8085 > /dev/null 2>&1; then
        echo "❌ Порт 8085 всё ещё занят!"
        lsof -i:8085
        exit 1
    fi
    echo "✅ Порт 8085 свободен"
ENDSSH

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Не удалось освободить порт${NC}"
    exit 1
fi


echo -e "${YELLOW}📤 Копирование на сервер...${NC}"
scp -i "$SSH_KEY" "$LOCAL_JAR" "${SERVER_USER}@${SERVER_IP}:~/${JAR_NAME}"
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Ошибка копирования${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Файл скопирован${NC}"


echo -e "${YELLOW}🚀 Запуск приложения...${NC}"
ssh -i "$SSH_KEY" "${SERVER_USER}@${SERVER_IP}" 'bash -s' << 'ENDSSH'
    rm -f ~/app_large.log
    
    echo "Запускаем новый процесс..."
    nohup java -jar ~/sauna_large.jar --spring.profiles.active=prod > ~/app_large.log 2>&1 &
    NEW_PID=$!
    
    echo "Ожидаем запуск (PID: $NEW_PID)..."
    sleep 5
    
    if ps -p $NEW_PID > /dev/null 2>&1; then
        echo "✅ Процесс запущен (PID: $NEW_PID)"
        sleep 3
        if lsof -ti:8085 > /dev/null 2>&1; then
            echo "✅ Приложение слушает порт 8085"
        else
            echo "⚠️ Порт 8085 пока не открыт, проверьте логи"
        fi
    else
        echo "❌ Процесс не запустился! Логи:"
        tail -30 ~/app_large.log
        exit 1
    fi
ENDSSH

echo -e "${GREEN}🎉 Деплой завершён успешно!${NC}"