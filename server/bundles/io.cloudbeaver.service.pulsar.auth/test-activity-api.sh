#!/bin/bash
#
# Activity Tracking API Test Script
#
# This script tests the Activity Tracking API endpoints
#

BASE_URL="${PULSEQL_URL:-http://localhost:8978}"
SESSION_ID="test-session-$(date +%s)"
USER_ID="test-user-123"

echo "==================================="
echo "Activity Tracking API Test Script"
echo "==================================="
echo ""
echo "Base URL: $BASE_URL"
echo "Session ID: $SESSION_ID"
echo "User ID: $USER_ID"
echo ""

# Test 1: Track a QUERY activity
echo "Test 1: Track QUERY activity"
echo "-----------------------------------"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/sso/activity" \
  -H "Content-Type: application/json" \
  -d "{
    \"session_id\": \"$SESSION_ID\",
    \"user_id\": \"$USER_ID\",
    \"event\": {
      \"type\": \"query\",
      \"timestamp\": $(date +%s)000,
      \"details\": {
        \"query_id\": \"q123\",
        \"connection_id\": \"conn-456\"
      }
    }
  }")
echo "Response: $RESPONSE"
echo ""

# Test 2: Track a NAVIGATION activity
echo "Test 2: Track NAVIGATION activity"
echo "-----------------------------------"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/sso/activity" \
  -H "Content-Type: application/json" \
  -d "{
    \"session_id\": \"$SESSION_ID\",
    \"user_id\": \"$USER_ID\",
    \"event\": {
      \"type\": \"navigation\",
      \"timestamp\": $(date +%s)000,
      \"details\": {
        \"page\": \"/workspace\",
        \"action\": \"view\"
      }
    }
  }")
echo "Response: $RESPONSE"
echo ""

# Test 3: Track an EXPORT activity
echo "Test 3: Track EXPORT activity"
echo "-----------------------------------"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/sso/activity" \
  -H "Content-Type: application/json" \
  -d "{
    \"session_id\": \"$SESSION_ID\",
    \"user_id\": \"$USER_ID\",
    \"event\": {
      \"type\": \"export\",
      \"timestamp\": $(date +%s)000,
      \"details\": {
        \"format\": \"csv\",
        \"rows\": 1000
      }
    }
  }")
echo "Response: $RESPONSE"
echo ""

# Test 4: Invalid session ID format
echo "Test 4: Invalid session ID (should fail)"
echo "-----------------------------------"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/sso/activity" \
  -H "Content-Type: application/json" \
  -d "{
    \"session_id\": \"invalid session with spaces\",
    \"user_id\": \"$USER_ID\",
    \"event\": {
      \"type\": \"query\",
      \"timestamp\": $(date +%s)000
    }
  }")
echo "Response: $RESPONSE"
echo ""

# Test 5: Missing session_id (should fail)
echo "Test 5: Missing session_id (should fail)"
echo "-----------------------------------"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/sso/activity" \
  -H "Content-Type: application/json" \
  -d "{
    \"user_id\": \"$USER_ID\",
    \"event\": {
      \"type\": \"query\",
      \"timestamp\": $(date +%s)000
    }
  }")
echo "Response: $RESPONSE"
echo ""

# Test 6: Missing event type (should fail)
echo "Test 6: Missing event type (should fail)"
echo "-----------------------------------"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/sso/activity" \
  -H "Content-Type: application/json" \
  -d "{
    \"session_id\": \"$SESSION_ID\",
    \"user_id\": \"$USER_ID\",
    \"event\": {
      \"timestamp\": $(date +%s)000
    }
  }")
echo "Response: $RESPONSE"
echo ""

# Test 7: Track IDLE activity
echo "Test 7: Track IDLE activity"
echo "-----------------------------------"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/sso/activity" \
  -H "Content-Type: application/json" \
  -d "{
    \"session_id\": \"$SESSION_ID\",
    \"user_id\": \"$USER_ID\",
    \"event\": {
      \"type\": \"idle\",
      \"timestamp\": $(date +%s)000,
      \"details\": {
        \"idle_duration\": 780000
      }
    }
  }")
echo "Response: $RESPONSE"
echo ""

# Test 8: Track ACTIVE activity after being idle
echo "Test 8: Track ACTIVE activity"
echo "-----------------------------------"
RESPONSE=$(curl -s -X POST "$BASE_URL/api/sso/activity" \
  -H "Content-Type: application/json" \
  -d "{
    \"session_id\": \"$SESSION_ID\",
    \"user_id\": \"$USER_ID\",
    \"event\": {
      \"type\": \"active\",
      \"timestamp\": $(date +%s)000
    }
  }")
echo "Response: $RESPONSE"
echo ""

echo "==================================="
echo "Tests completed!"
echo "==================================="
