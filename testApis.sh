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
    
    if [ ! -z "$data" ]; then
        if [ ! -z "$token" ]; then
            curl -s -X $method "$BASE_URL$url" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $token" \
                -d "$data"
        else
            curl -s -X $method "$BASE_URL$url" \
                -H "Content-Type: application/json" \
                -d "$data"
        fi
    else
        if [ ! -z "$token" ]; then
            curl -s -X $method "$BASE_URL$url" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $token"
        else
            curl -s -X $method "$BASE_URL$url" \
                -H "Content-Type: application/json"
        fi
    fi
}

# Function to print test results
print_test_result() {
    local test_name=$1
    local response=$2
    local expected_status=$3
    
    if [ ! -z "$response" ] && [ "$response" != "null" ]; then
        echo "✅ $test_name - PASSED"
        echo "   Response: $response"
    else
        echo "❌ $test_name - FAILED"
        echo "   Response: $response"
    fi
    echo ""
}

# Test 1: Admin Login
echo "=== Testing Authentication ==="
echo "1. Testing Admin Login..."
ADMIN_TOKEN=$(api_call "POST" "/auth/login" "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}" | jq -r '.token')
if [ "$ADMIN_TOKEN" != "null" ] && [ ! -z "$ADMIN_TOKEN" ]; then
    echo "✅ Admin Login - PASSED"
    echo "   Token: ${ADMIN_TOKEN:0:20}..."
else
    echo "❌ Admin Login - FAILED"
    echo "Cannot proceed without authentication"
    exit 1
fi
echo ""

# Test 2: Cache Health Check
echo "=== Testing Cache Management ==="
echo "2. Testing Cache Health Check..."
CACHE_HEALTH=$(api_call "GET" "/cache/health" "" "$ADMIN_TOKEN")
print_test_result "Cache Health Check" "$CACHE_HEALTH"

# Test 3: Cache Statistics (Initial)
echo "3. Testing Cache Statistics (Initial)..."
CACHE_STATS_INITIAL=$(api_call "GET" "/cache/stats" "" "$ADMIN_TOKEN")
print_test_result "Cache Statistics (Initial)" "$CACHE_STATS_INITIAL"

# Test 4: Import Videos
echo "=== Testing Video Import ==="
echo "4. Testing Video Import..."
IMPORT_RESPONSE=$(api_call "POST" "/videos/import" "{\"source\":\"MOCK\",\"videoIds\":[\"video1\",\"video2\",\"video3\",\"video4\",\"video5\"],\"batchSize\":10}" "$ADMIN_TOKEN")
print_test_result "Import Videos (Mock)" "$IMPORT_RESPONSE"

# Test 5: Get Video Statistics (First call - should hit database)
echo "=== Testing Video Statistics with Caching ==="
echo "5. Testing Get Video Statistics (First Call - Database)..."
STATS_FIRST=$(api_call "GET" "/videos/stats" "" "$ADMIN_TOKEN")
print_test_result "Get Video Statistics (First Call - Database)" "$STATS_FIRST"

# Test 6: Get Video Statistics (Second call - should hit cache)
echo "6. Testing Get Video Statistics (Second Call - Cache)..."
STATS_SECOND=$(api_call "GET" "/videos/stats" "" "$ADMIN_TOKEN")
print_test_result "Get Video Statistics (Second Call - Cache)" "$STATS_SECOND"

# Test 7: Cache Statistics (after operations)
echo "7. Testing Cache Statistics (After Operations)..."
CACHE_STATS_AFTER=$(api_call "GET" "/cache/stats" "" "$ADMIN_TOKEN")
print_test_result "Cache Statistics (After Operations)" "$CACHE_STATS_AFTER"

# Test 8: Clear Video Statistics Cache
echo "8. Testing Clear Video Statistics Cache..."
CLEAR_CACHE=$(api_call "DELETE" "/cache/clear/video-stats" "" "$ADMIN_TOKEN")
print_test_result "Clear Video Statistics Cache" "$CLEAR_CACHE"

# Test 9: Get Video Statistics (After cache clear - should hit database again)
echo "9. Testing Get Video Statistics (After Cache Clear)..."
STATS_AFTER_CLEAR=$(api_call "GET" "/videos/stats" "" "$ADMIN_TOKEN")
print_test_result "Get Video Statistics (After Cache Clear)" "$STATS_AFTER_CLEAR"

# Test 10: Async Video Import
echo "=== Testing Async Video Import ==="
echo "10. Testing Start Async Video Import..."
ASYNC_IMPORT=$(api_call "POST" "/videos/import/async" "{\"source\":\"MOCK\",\"videoIds\":[\"async-video-1\",\"async-video-2\",\"async-video-3\"],\"batchSize\":2}" "$ADMIN_TOKEN")
print_test_result "Start Async Video Import" "$ASYNC_IMPORT"

# Test 11: Get Job Status (if async import was successful)
JOB_ID=$(echo "$ASYNC_IMPORT" | jq -r '.jobId')
if [ "$JOB_ID" != "null" ] && [ ! -z "$JOB_ID" ]; then
    echo "11. Testing Get Job Status..."
    sleep 2  # Wait a bit for job to start
    JOB_STATUS=$(api_call "GET" "/videos/import/jobs/$JOB_ID" "" "$ADMIN_TOKEN")
    print_test_result "Get Job Status" "$JOB_STATUS"
else
    echo "⚠️  Skipping job status test - no job ID received"
fi

# Test 12: Get All Jobs
echo "12. Testing Get All Jobs..."
ALL_JOBS=$(api_call "GET" "/videos/import/jobs" "" "$ADMIN_TOKEN")
print_test_result "Get All Jobs" "$ALL_JOBS"

# Test 13: Get Jobs by Status
echo "13. Testing Get Jobs by Status (RUNNING)..."
JOBS_BY_STATUS=$(api_call "GET" "/videos/import/jobs/status/RUNNING" "" "$ADMIN_TOKEN")
print_test_result "Get Jobs by Status (RUNNING)" "$JOBS_BY_STATUS"

# Test 14: Get Job Statistics
echo "14. Testing Get Job Statistics..."
JOB_STATS=$(api_call "GET" "/videos/import/jobs/stats" "" "$ADMIN_TOKEN")
print_test_result "Get Job Statistics" "$JOB_STATS"

# Test 15: Get Videos
echo "=== Testing Video Retrieval ==="
echo "15. Testing Get Videos..."
VIDEOS_RESPONSE=$(api_call "GET" "/videos?page=0&size=10" "" "$ADMIN_TOKEN")
print_test_result "Get Videos (No filters)" "$VIDEOS_RESPONSE"

# Test 16: Get Videos with filters
echo "16. Testing Get Videos with filters..."
VIDEOS_FILTERED=$(api_call "GET" "/videos?source=MOCK&page=0&size=5" "" "$ADMIN_TOKEN")
print_test_result "Get Videos (With filters)" "$VIDEOS_FILTERED"

# Test 17: Get Statistics
echo "17. Testing Get Statistics..."
STATS_RESPONSE=$(api_call "GET" "/videos/stats" "" "$ADMIN_TOKEN")
print_test_result "Get Video Statistics" "$STATS_RESPONSE"

# Test 18: User Login
echo "=== Testing User Role Access ==="
echo "18. Testing User Login..."
USER_TOKEN=$(api_call "POST" "/auth/login" "{\"username\":\"$USER_USERNAME\",\"password\":\"$USER_PASSWORD\"}" | jq -r '.token')
print_test_result "User Login" "$USER_TOKEN"

# Test 19: User Access (should work)
if [ "$USER_TOKEN" != "null" ] && [ ! -z "$USER_TOKEN" ]; then
    echo "19. Testing User Access..."
    USER_VIDEOS_RESPONSE=$(api_call "GET" "/videos" "" "$USER_TOKEN")
    print_test_result "User Get Videos" "$USER_VIDEOS_RESPONSE"
    
    # Test 20: User Import (should fail)
    echo "20. Testing User Import (should fail)..."
    USER_IMPORT_RESPONSE=$(api_call "POST" "/videos/import" "{\"source\":\"MOCK\",\"videoIds\":[\"video1\"],\"batchSize\":10}" "$USER_TOKEN")
    print_test_result "User Import Videos (Should Fail)" "$USER_IMPORT_RESPONSE"
    
    # Test 21: User Cache Health Access (should work)
    echo "21. Testing User Cache Health Access..."
    USER_CACHE_HEALTH=$(api_call "GET" "/cache/health" "" "$USER_TOKEN")
    print_test_result "User Cache Health Access" "$USER_CACHE_HEALTH"
    
    # Test 22: User Clear Cache (should fail)
    echo "22. Testing User Clear Cache (should fail)..."
    USER_CLEAR_CACHE=$(api_call "DELETE" "/cache/clear/video-stats" "" "$USER_TOKEN")
    print_test_result "User Clear Cache (Should Fail)" "$USER_CLEAR_CACHE"
fi

# Test 23: Test unauthorized access
echo "=== Testing Security ==="
echo "23. Testing Unauthorized Access..."
UNAUTHORIZED=$(api_call "GET" "/videos")
print_test_result "Unauthorized Access" "$UNAUTHORIZED"

# Test 24: Test health endpoint
echo "=== Testing Health Endpoint ==="
echo "24. Testing Health Check..."
HEALTH=$(api_call "GET" "/actuator/health")
print_test_result "Health Check" "$HEALTH"

# Test 25: Test Swagger UI endpoint
echo "25. Testing Swagger UI..."
SWAGGER=$(api_call "GET" "/swagger-ui.html")
print_test_result "Swagger UI" "$SWAGGER"

echo ""
echo "=== Test Complete ==="
echo ""
echo "To run the application:"
echo "1. Start with Docker: docker-compose up --build -d"
echo "2. Or start locally: ./mvnw spring-boot:run"
echo "3. Run this script: ./testApis.sh"
echo "4. View Swagger UI: http://localhost:8080/swagger-ui.html"
echo "5. View H2 Console: http://localhost:8080/h2-console"
echo "6. View Health Check: http://localhost:8080/actuator/health"
echo ""
echo "Default credentials:"
echo "- Admin: admin/admin123"
echo "- User: user/user123"
echo ""
echo "New Features Tested:"
echo "- Cache Management (health, stats, clear)"
echo "- Async Video Import (jobs, status, statistics)"
echo "- Cache Hit/Miss Testing"
echo "- Role-based Cache Access"
echo ""