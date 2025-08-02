# Video Metadata Service API Test Script (PowerShell)
# Comprehensive testing of all API endpoints

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "admin123",
    [string]$UserUsername = "user",
    [string]$UserPassword = "user123"
)

Write-Host "=== Video Metadata Service API Test Script ===" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "Admin Username: $AdminUsername" -ForegroundColor Yellow
Write-Host "User Username: $UserUsername" -ForegroundColor Yellow
Write-Host ""

# Function to make API calls
function Invoke-ApiCall {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Data = $null,
        [string]$Token = $null
    )
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    
    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Url"
        Headers = $headers
    }
    
    if ($Data) {
        $params.Body = $Data
    }
    
    try {
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
            $jsonResponse = $Result.Data | ConvertTo-Json -Depth 2
            Write-Host "   Response: $jsonResponse" -ForegroundColor Gray
        }
    } else {
        Write-Host "❌ $TestName - FAILED" -ForegroundColor Red
        Write-Host "   Error: $($Result.Error)" -ForegroundColor Red
        Write-Host "   Status Code: $($Result.StatusCode)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 1: Admin Login
Write-Host "=== Testing Authentication ===" -ForegroundColor Cyan
$adminLoginBody = @{
    username = $AdminUsername
    password = $AdminPassword
} | ConvertTo-Json

$adminLoginResult = Invoke-ApiCall -Method "POST" -Url "/auth/login" -Data $adminLoginBody
Write-TestResult -TestName "Admin Login" -Result $adminLoginResult

if (-not $adminLoginResult.Success) {
    Write-Host "❌ Authentication failed. Cannot proceed with other tests." -ForegroundColor Red
    exit 1
}

$adminToken = $adminLoginResult.Data.token
Write-Host "✅ Authentication successful. Token obtained." -ForegroundColor Green
Write-Host ""

# Test 2: Import Videos (Mock)
Write-Host "=== Testing Video Import ===" -ForegroundColor Cyan
$importBody = @{
    source = "MOCK"
    videoIds = @("test-video-1", "test-video-2", "test-video-3", "test-video-4", "test-video-5")
    batchSize = 10
} | ConvertTo-Json

$importResult = Invoke-ApiCall -Method "POST" -Url "/videos/import" -Data $importBody -Token $adminToken
Write-TestResult -TestName "Import Videos (Mock)" -Result $importResult

# Test 3: Get Videos (No filters)
Write-Host "=== Testing Video Retrieval ===" -ForegroundColor Cyan
$getVideosResult = Invoke-ApiCall -Method "GET" -Url "/videos?page=0&size=10" -Token $adminToken
Write-TestResult -TestName "Get Videos (No filters)" -Result $getVideosResult

# Test 4: Get Videos with filters
$getVideosFilteredResult = Invoke-ApiCall -Method "GET" -Url "/videos?source=MOCK&page=0&size=5" -Token $adminToken
Write-TestResult -TestName "Get Videos (With filters)" -Result $getVideosFilteredResult

# Test 5: Get Video Statistics
Write-Host "=== Testing Video Statistics ===" -ForegroundColor Cyan
$statsResult = Invoke-ApiCall -Method "GET" -Url "/videos/stats" -Token $adminToken
Write-TestResult -TestName "Get Video Statistics" -Result $statsResult

# Test 6: Get Single Video (if videos exist)
if ($getVideosResult.Success -and $getVideosResult.Data.content -and $getVideosResult.Data.content.Count -gt 0) {
    $firstVideoId = $getVideosResult.Data.content[0].id
    $getSingleVideoResult = Invoke-ApiCall -Method "GET" -Url "/videos/$firstVideoId" -Token $adminToken
    Write-TestResult -TestName "Get Single Video" -Result $getSingleVideoResult
} else {
    Write-Host "⚠️  Skipping single video test - no videos available" -ForegroundColor Yellow
}

# Test 7: Test unauthorized access
Write-Host "=== Testing Security ===" -ForegroundColor Cyan
$unauthorizedResult = Invoke-ApiCall -Method "GET" -Url "/videos"
Write-TestResult -TestName "Unauthorized Access" -Result $unauthorizedResult

# Test 8: Test invalid import request
$invalidImportBody = @{
    source = "INVALID_SOURCE"
    videoIds = @("test-video-1")
} | ConvertTo-Json

$invalidImportResult = Invoke-ApiCall -Method "POST" -Url "/videos/import" -Data $invalidImportBody -Token $adminToken
Write-TestResult -TestName "Invalid Import Request" -Result $invalidImportResult

# Test 9: Test non-existent video
$nonExistentResult = Invoke-ApiCall -Method "GET" -Url "/videos/999999" -Token $adminToken
Write-TestResult -TestName "Non-existent Video" -Result $nonExistentResult

# Test 10: Test with different user role
Write-Host "=== Testing User Role Access ===" -ForegroundColor Cyan
$userLoginBody = @{
    username = $UserUsername
    password = $UserPassword
} | ConvertTo-Json

$userLoginResult = Invoke-ApiCall -Method "POST" -Url "/auth/login" -Data $userLoginBody
Write-TestResult -TestName "User Login" -Result $userLoginResult

if ($userLoginResult.Success) {
    $userToken = $userLoginResult.Data.token
    
    # User should be able to get videos
    $userGetVideosResult = Invoke-ApiCall -Method "GET" -Url "/videos" -Token $userToken
    Write-TestResult -TestName "User Get Videos" -Result $userGetVideosResult
    
    # User should NOT be able to import videos
    $userImportResult = Invoke-ApiCall -Method "POST" -Url "/videos/import" -Data $importBody -Token $userToken
    Write-TestResult -TestName "User Import Videos (Should Fail)" -Result $userImportResult
}

# Test 11: Test pagination
Write-Host "=== Testing Pagination ===" -ForegroundColor Cyan
$paginationResult = Invoke-ApiCall -Method "GET" -Url "/videos?page=0&size=5&sort=title&direction=ASC" -Token $adminToken
Write-TestResult -TestName "Pagination Test" -Result $paginationResult

# Test 12: Test date filters
$dateFilterResult = Invoke-ApiCall -Method "GET" -Url "/videos?uploadDateFrom=2023-01-01T00:00:00&uploadDateTo=2025-12-31T23:59:59" -Token $adminToken
Write-TestResult -TestName "Date Filter Test" -Result $dateFilterResult

# Test 13: Test duration filters
$durationFilterResult = Invoke-ApiCall -Method "GET" -Url "/videos?minDuration=60&maxDuration=3600" -Token $adminToken
Write-TestResult -TestName "Duration Filter Test" -Result $durationFilterResult

# Test 14: Test validation errors
Write-Host "=== Testing Validation ===" -ForegroundColor Cyan
$validationErrorBody = @{
    source = "MOCK"
    videoIds = @()
    batchSize = 0
} | ConvertTo-Json

$validationResult = Invoke-ApiCall -Method "POST" -Url "/videos/import" -Data $validationErrorBody -Token $adminToken
Write-TestResult -TestName "Validation Error Test" -Result $validationResult

# Test 15: Test with invalid token
Write-Host "=== Testing Invalid Token ===" -ForegroundColor Cyan
$invalidTokenResult = Invoke-ApiCall -Method "GET" -Url "/videos" -Token "invalid-token-here"
Write-TestResult -TestName "Invalid Token Test" -Result $invalidTokenResult

# Test 16: Test health endpoint
Write-Host "=== Testing Health Endpoint ===" -ForegroundColor Cyan
$healthResult = Invoke-ApiCall -Method "GET" -Url "/actuator/health"
Write-TestResult -TestName "Health Check" -Result $healthResult

# Test 17: Test info endpoint
$infoResult = Invoke-ApiCall -Method "GET" -Url "/actuator/info"
Write-TestResult -TestName "Info Endpoint" -Result $infoResult

# Test 18: Test Swagger UI endpoint
$swaggerResult = Invoke-ApiCall -Method "GET" -Url "/swagger-ui.html"
Write-TestResult -TestName "Swagger UI" -Result $swaggerResult

# Test 19: Test API docs endpoint
$apiDocsResult = Invoke-ApiCall -Method "GET" -Url "/v3/api-docs"
Write-TestResult -TestName "API Documentation" -Result $apiDocsResult

# Test 20: Test H2 Console (should redirect or show login)
$h2ConsoleResult = Invoke-ApiCall -Method "GET" -Url "/h2-console"
Write-TestResult -TestName "H2 Console" -Result $h2ConsoleResult

Write-Host "=== API Test Summary ===" -ForegroundColor Green
Write-Host "All tests completed. Check the results above." -ForegroundColor Yellow
Write-Host ""
Write-Host "To run the application:" -ForegroundColor Cyan
Write-Host "1. Start the application: ./mvnw spring-boot:run" -ForegroundColor White
Write-Host "2. Run this script: ./test-api-complete.ps1" -ForegroundColor White
Write-Host "3. View Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "4. View H2 Console: http://localhost:8080/h2-console" -ForegroundColor White
Write-Host "5. View Health Check: http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "Default credentials:" -ForegroundColor Cyan
Write-Host "- Admin: admin/admin123" -ForegroundColor White
Write-Host "- User: user/user123" -ForegroundColor White
Write-Host "" 