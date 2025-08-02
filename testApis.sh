#!/bin/bash

BASE_URL="http://localhost:8080"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="admin123"
USER_USERNAME="user"
USER_PASSWORD="user123"

echo "=== Video Metadata Service API Test ==="
echo "Base URL: $BASE_URL"
echo ""

# Function to make API calls
api_call() {
    local method=$1
    local url=$2
    local data=$3
    local token=$4
    
    local headers="Content-Type: application/json"
    if [ ! -z "$token" ]; then
        headers="$headers, Authorization: Bearer $token"
    fi
    
    if [ ! -z "$data" ]; then
        curl -s -X $method "$BASE_URL$url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token" \
            -d "$data"
    else
        curl -s -X $method "$BASE_URL$url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token"
    fi
}

# Test 1: Admin Login
echo "1. Testing Admin Login..."
ADMIN_TOKEN=$(api_call "POST" "/auth/login" "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}" | jq -r '.token')
echo "Admin token: ${ADMIN_TOKEN:0:20}..."

# Test 2: Import Videos
echo "2. Testing Video Import..."
IMPORT_RESPONSE=$(api_call "POST" "/videos/import" "{\"source\":\"MOCK\",\"videoIds\":[\"video1\",\"video2\",\"video3\"],\"batchSize\":10}" "$ADMIN_TOKEN")
echo "Import response: $IMPORT_RESPONSE"

# Test 3: Get Videos
echo "3. Testing Get Videos..."
VIDEOS_RESPONSE=$(api_call "GET" "/videos?page=0&size=10" "" "$ADMIN_TOKEN")
echo "Videos response: $VIDEOS_RESPONSE"

# Test 4: Get Statistics
echo "4. Testing Get Statistics..."
STATS_RESPONSE=$(api_call "GET" "/videos/stats" "" "$ADMIN_TOKEN")
echo "Statistics response: $STATS_RESPONSE"

# Test 5: User Login
echo "5. Testing User Login..."
USER_TOKEN=$(api_call "POST" "/auth/login" "{\"username\":\"$USER_USERNAME\",\"password\":\"$USER_PASSWORD\"}" | jq -r '.token')
echo "User token: ${USER_TOKEN:0:20}..."

# Test 6: User Access (should work)
echo "6. Testing User Access..."
USER_VIDEOS_RESPONSE=$(api_call "GET" "/videos" "" "$USER_TOKEN")
echo "User videos response: $USER_VIDEOS_RESPONSE"

# Test 7: User Import (should fail)
echo "7. Testing User Import (should fail)..."
USER_IMPORT_RESPONSE=$(api_call "POST" "/videos/import" "{\"source\":\"MOCK\",\"videoIds\":[\"video1\"],\"batchSize\":10}" "$USER_TOKEN")
echo "User import response: $USER_IMPORT_RESPONSE"

echo ""
echo "=== Test Complete ==="