#!/bin/bash

SERVER_USER="webpanel"
SERVER_IP="212.164.86.237"
JAR_NAME="sauna_large.jar"
LOCAL_JAR="backend/target/sauna_large.jar"
SSH_KEY="$HOME/.ssh/deploy_key"

# !!! ВАЖНО: Путь, откуда Systemd запускает файл
REMOTE_DEST_DIR="/opt/webpanel"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}🚀 Deploy started...${NC}"

echo -e "${YELLOW}📦 Maven Build...${NC}"
cd backend
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Maven build failed${NC}"
    exit 1
fi
cd ..
echo -e "${GREEN}✅ Build success${NC}"

echo -e "${YELLOW}📤 Uploading new jar to ${REMOTE_DEST_DIR}...${NC}"
# Копируем прямо в рабочую папку сервиса
scp -i "$SSH_KEY" "$LOCAL_JAR" "${SERVER_USER}@${SERVER_IP}:${REMOTE_DEST_DIR}/${JAR_NAME}"
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ SCP failed. Check permissions for writing to /opt/webpanel${NC}"
    exit 1
fi
echo -e "${GREEN}✅ File uploaded${NC}"

echo -e "${YELLOW}🔄 Triggering Systemd Restart (Kill process)...${NC}"
ssh -i "$SSH_KEY" "${SERVER_USER}@${SERVER_IP}" 'bash -s' << 'ENDSSH'
    # Убиваем старый процесс
    PORT_PID=$(lsof -ti:8085)
    if [ -n "$PORT_PID" ]; then
        echo "Killing PID: $PORT_PID"
        kill $PORT_PID
    else
        echo "Process not found on port 8085, maybe already dead."
    fi
    
    # На всякий случай добиваем по имени
    pkill -f "sauna_large.jar"
    
    echo "Waiting for Systemd to restart application (RestartSec=5s in config)..."
    
    # Ждем до 60 секунд (30 * 2)
    for i in {1..90}; do
        sleep 2
        if lsof -ti:8085 > /dev/null 2>&1; then
            NEW_PID=$(lsof -ti:8085)
            echo "✅ Service restarted automatically! PID: $NEW_PID"
            exit 0
        fi
        echo -n "."
    done
    
    echo ""
    echo -e "⚠️ Timeout! Check logs at: /opt/webpanel/app_sauna_large.log"
    exit 1
ENDSSH

if [ $? -eq 0 ]; then
    echo -e "${GREEN}🎉 Deploy successful!${NC}"
else
    echo -e "${RED}❌ Service did not start correctly${NC}"
    exit 1
fi