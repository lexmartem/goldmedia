# Video Metadata Service API Test Script
# This script tests all endpoints of the Video Metadata Service

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "admin123"
)

Write-Host "=== Video Metadata Service API Test Script ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "Username: $Username" -ForegroundColor Yellow
Write-Host ""

# Function to make HTTP requests
function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Body = $null,
        [hashtable]$Headers = @{}
    )
    
    try {
        $params = @{
            Method = $Method
            Uri = $Url
            Headers = $Headers
            ContentType = "application/json"
        }
        
        if ($Body) {
            $params.Body = $Body
        }
        
        $response = Invoke-RestMethod @params
        return @{
            Success = $true
            Data = $response
            StatusCode = 200
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $errorMessage = $_.Exception.Message
        return @{
            Success = $false
            Error = $errorMessage
            StatusCode = $statusCode
        }
    }
}

# Function to print test results
function Write-TestResult {
    param(
        [string]$TestName,
        [hashtable]$Result
    )
    
    if ($Result.Success) {
        Write-Host "✅ $TestName - PASSED" -ForegroundColor Green
        if ($Result.Data) {
            Write-Host "   Response: $($Result.Data | ConvertTo-Json -Depth 2)" -ForegroundColor Gray
        }
    } else {
        Write-Host "❌ $TestName - FAILED" -ForegroundColor Red
        Write-Host "   Error: $($Result.Error)" -ForegroundColor Red
        Write-Host "   Status Code: $($Result.StatusCode)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 1: Login
Write-Host "=== Testing Authentication ===" -ForegroundColor Cyan
$loginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

$loginResult = Invoke-ApiRequest -Method "POST" -Url "$BaseUrl/auth/login" -Body $loginBody
Write-TestResult -TestName "Login" -Result $loginResult

if (-not $loginResult.Success) {
    Write-Host "❌ Authentication failed. Cannot proceed with other tests." -ForegroundColor Red
    exit 1
}

$token = $loginResult.Data.token
Write-Host "✅ Authentication successful. Token obtained." -ForegroundColor Green
Write-Host ""

# Test 2: Import Videos (Mock)
Write-Host "=== Testing Video Import ===" -ForegroundColor Cyan
$importBody = @{
    source = "MOCK"
    videoIds = @("test-video-1", "test-video-2", "test-video-3")
    batchSize = 10
} | ConvertTo-Json

$importHeaders = @{
    "Authorization" = "Bearer $token"
}

$importResult = Invoke-ApiRequest -Method "POST" -Url "$BaseUrl/videos/import" -Body $importBody -Headers $importHeaders
Write-TestResult -TestName "Import Videos (Mock)" -Result $importResult

# Test 3: Get Videos (No filters)
Write-Host "=== Testing Video Retrieval ===" -ForegroundColor Cyan
$getVideosResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos?page=0&size=10" -Headers $importHeaders
Write-TestResult -TestName "Get Videos (No filters)" -Result $getVideosResult

# Test 4: Get Videos with filters
$getVideosFilteredResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos?source=MOCK&page=0&size=5" -Headers $importHeaders
Write-TestResult -TestName "Get Videos (With filters)" -Result $getVideosFilteredResult

# Test 5: Get Video Statistics
Write-Host "=== Testing Video Statistics ===" -ForegroundColor Cyan
$statsResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos/stats" -Headers $importHeaders
Write-TestResult -TestName "Get Video Statistics" -Result $statsResult

# Test 6: Get Single Video (if videos exist)
if ($getVideosResult.Success -and $getVideosResult.Data.content -and $getVideosResult.Data.content.Count -gt 0) {
    $firstVideoId = $getVideosResult.Data.content[0].id
    $getSingleVideoResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos/$firstVideoId" -Headers $importHeaders
    Write-TestResult -TestName "Get Single Video" -Result $getSingleVideoResult
} else {
    Write-Host "⚠️  Skipping single video test - no videos available" -ForegroundColor Yellow
}

# Test 7: Test unauthorized access
Write-Host "=== Testing Security ===" -ForegroundColor Cyan
$unauthorizedResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos"
Write-TestResult -TestName "Unauthorized Access" -Result $unauthorizedResult

# Test 8: Test invalid import request
$invalidImportBody = @{
    source = "INVALID_SOURCE"
    videoIds = @("test-video-1")
} | ConvertTo-Json

$invalidImportResult = Invoke-ApiRequest -Method "POST" -Url "$BaseUrl/videos/import" -Body $invalidImportBody -Headers $importHeaders
Write-TestResult -TestName "Invalid Import Request" -Result $invalidImportResult

# Test 9: Test non-existent video
$nonExistentResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos/999999" -Headers $importHeaders
Write-TestResult -TestName "Non-existent Video" -Result $nonExistentResult

# Test 10: Test with different user role
Write-Host "=== Testing User Role Access ===" -ForegroundColor Cyan
$userLoginBody = @{
    username = "user"
    password = "user123"
} | ConvertTo-Json

$userLoginResult = Invoke-ApiRequest -Method "POST" -Url "$BaseUrl/auth/login" -Body $userLoginBody
Write-TestResult -TestName "User Login" -Result $userLoginResult

if ($userLoginResult.Success) {
    $userToken = $userLoginResult.Data.token
    $userHeaders = @{
        "Authorization" = "Bearer $userToken"
    }
    
    # User should be able to get videos
    $userGetVideosResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos" -Headers $userHeaders
    Write-TestResult -TestName "User Get Videos" -Result $userGetVideosResult
    
    # User should NOT be able to import videos
    $userImportResult = Invoke-ApiRequest -Method "POST" -Url "$BaseUrl/videos/import" -Body $importBody -Headers $userHeaders
    Write-TestResult -TestName "User Import Videos (Should Fail)" -Result $userImportResult
}

# Test 11: Test pagination
Write-Host "=== Testing Pagination ===" -ForegroundColor Cyan
$paginationResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos?page=0&size=5&sort=title&direction=ASC" -Headers $importHeaders
Write-TestResult -TestName "Pagination Test" -Result $paginationResult

# Test 12: Test date filters
$dateFilterResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos?uploadDateFrom=2023-01-01T00:00:00&uploadDateTo=2025-12-31T23:59:59" -Headers $importHeaders
Write-TestResult -TestName "Date Filter Test" -Result $dateFilterResult

# Test 13: Test duration filters
$durationFilterResult = Invoke-ApiRequest -Method "GET" -Url "$BaseUrl/videos?minDuration=60&maxDuration=3600" -Headers $importHeaders
Write-TestResult -TestName "Duration Filter Test" -Result $durationFilterResult

Write-Host "=== API Test Summary ===" -ForegroundColor Green
Write-Host "All tests completed. Check the results above." -ForegroundColor Yellow
Write-Host ""
Write-Host "To run the application:" -ForegroundColor Cyan
Write-Host "1. Start the application: ./mvnw spring-boot:run" -ForegroundColor White
Write-Host "2. Run this script: ./test-api.ps1" -ForegroundColor White
Write-Host "3. View Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "" 