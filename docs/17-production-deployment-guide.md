# Deployment Guide for Pulsar-PulseQL Integration

**Date**: December 13, 2025  
**Version**: 1.0 - Production Deployment  
**Status**: Complete

## Overview

This guide provides step-by-step instructions for deploying the Pulsar-PulseQL integration to production with full SSO support, HTTPS, and security hardening.

## Prerequisites

### Required Software
- Java 11 or higher
- Node.js 18+ and npm
- nginx 1.20+
- PostgreSQL 12+ (for CloudBeaver metadata)
- Redis 6+ (optional, for distributed rate limiting)

### Required Access
- SSL certificate for domain
- Access to Pulsar SSO signing secret
- Server with ports 80, 443, 8978 available

## Deployment Steps

### Step 1: Backend Deployment

#### 1.1 Build Backend

```bash
cd /path/to/pulseql/server
mvn clean package -DskipTests

# Output: server/product/web-server/target/cloudbeaver-web-server-*.war
```

#### 1.2 Configure Signing Secret

**Option A: Environment Variable (Recommended)**
```bash
export PULSAR_SSO_SECRET="your-256-bit-secret-key-from-pulsar"
```

**Option B: Configuration File**
Edit `cloudbeaver.conf`:
```json
{
  "pulsar": {
    "sso": {
      "signingSecret": "your-actual-secret-here"
    }
  }
}
```

**CRITICAL**: The secret must match the one used by Pulsar for JWT signing.

#### 1.3 Deploy Backend

```bash
# Copy to deployment location
cp cloudbeaver-web-server-*.war /opt/pulseql/

# Extract war
cd /opt/pulseql
jar xf cloudbeaver-web-server-*.war

# Copy configuration
cp /path/to/deploy/conf/cloudbeaver.conf conf/

# Start server
./run-server.sh

# Verify: http://localhost:8978/health
```

### Step 2: Frontend Deployment

#### 2.1 Build Frontend

```bash
cd /path/to/pulseql/webapp
npm install
npm run build

# Output: webapp/packages/app/dist/
```

#### 2.2 Deploy Frontend

```bash
# Copy to web root
cp -r webapp/packages/app/dist/* /var/www/pulseql/public/

# Set permissions
chown -R www-data:www-data /var/www/pulseql
chmod -R 755 /var/www/pulseql
```

### Step 3: HTTPS Configuration

#### 3.1 Obtain SSL Certificate

**Option A: Let's Encrypt (Free)**
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d pulseql.example.com
```

**Option B: Commercial Certificate**
```bash
# Place certificate files
sudo cp pulseql.crt /etc/ssl/certs/
sudo cp pulseql.key /etc/ssl/private/
sudo chmod 600 /etc/ssl/private/pulseql.key
```

#### 3.2 Configure nginx

```bash
# Copy nginx configuration
sudo cp /path/to/deploy/nginx/pulseql-ssl.conf /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/pulseql-ssl.conf /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Reload nginx
sudo systemctl reload nginx
```

### Step 4: Rate Limiting

Rate limiting is configured in nginx (already in pulseql-ssl.conf):
- SSO endpoints: 10 req/min per IP
- API endpoints: 30 req/min per IP
- General: 100 req/min per IP

**Monitoring**:
```bash
# Check rate limit errors
sudo tail -f /var/log/nginx/pulseql_error.log | grep "limiting requests"
```

### Step 5: Security Hardening

#### 5.1 Firewall Configuration

```bash
# Allow only HTTPS
sudo ufw allow 443/tcp
sudo ufw allow 80/tcp  # For redirect only
sudo ufw deny 8978/tcp # Block direct backend access
sudo ufw enable
```

#### 5.2 Secret Rotation

Create rotation script (`/opt/pulseql/scripts/rotate-secret.sh`):
```bash
#!/bin/bash
# Generate new 256-bit secret
NEW_SECRET=$(openssl rand -base64 32)

# Update configuration
sed -i "s/signingSecret: .*/signingSecret: \"$NEW_SECRET\"/" /opt/pulseql/conf/cloudbeaver.conf

# Restart server
systemctl restart pulseql

echo "Secret rotated. Coordinate with Pulsar team to update their secret."
echo "New secret: $NEW_SECRET"
```

#### 5.3 Audit Logging

Configure Log4j2 (`conf/log4j2.xml`):
```xml
<Appender name="SSOAudit" type="RollingFile" fileName="logs/sso-audit.log">
    <PatternLayout pattern="%d{ISO8601} [%t] %-5p %c{1} - %m%n"/>
    <Policies>
        <TimeBasedTriggeringPolicy interval="1"/>
        <SizeBasedTriggeringPolicy size="100MB"/>
    </Policies>
</Appender>

<Logger name="io.cloudbeaver.service.pulsar.auth" level="INFO" additivity="false">
    <AppenderRef ref="SSOAudit"/>
</Logger>
```

### Step 6: Monitoring Setup

#### 6.1 Health Checks

Create health check script:
```bash
#!/bin/bash
curl -f https://pulseql.example.com/health || exit 1
```

#### 6.2 Metrics Collection

Monitor these endpoints:
- SSO validation success rate
- Token blacklist size
- API response times
- Error rates

**Prometheus Metrics** (add to backend):
```java
// SSO metrics
Counter ssoValidationSuccess;
Counter ssoValidationFailure;
Gauge tokenBlacklistSize;
Histogram ssoValidationDuration;
```

#### 6.3 Alerting

Set up alerts for:
- SSO success rate < 95%
- SSO validation time > 500ms (p95)
- Blacklist size > 10,000
- Error rate > 5%

### Step 7: Testing

#### 7.1 SSO Flow Test

```bash
# 1. Get token from Pulsar (coordinate with Pulsar team)
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 2. Test validation endpoint
curl -X POST https://pulseql.example.com/api/sso/validate \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"$TOKEN\"}"

# Expected: {"success":true,"userId":"...","displayName":"..."}
```

#### 7.2 Security Test

```bash
# Test rate limiting
for i in {1..15}; do
  curl -X POST https://pulseql.example.com/api/sso/validate \
    -H "Content-Type: application/json" \
    -d '{"token":"test"}'
done

# Expected: 429 Too Many Requests after 10 attempts

# Test HTTPS redirect
curl -I http://pulseql.example.com
# Expected: 301 redirect to https://

# Test security headers
curl -I https://pulseql.example.com
# Expected: HSTS, X-Frame-Options, CSP headers
```

#### 7.3 Integration Test

```bash
# Full workflow test
1. Open: https://pulseql.example.com/workspace?mode=pulsar&sso_token=<JWT>
2. Verify: User authenticated automatically
3. Verify: Workspace customized with Pulsar branding
4. Verify: Permissions enforced (based on token)
5. Test: "Back to Pulsar" button works
6. Test: Logout revokes token
```

## Production Checklist

### Pre-Deployment ✅
- [ ] SSL certificate obtained
- [ ] Signing secret configured
- [ ] Backend built and tested
- [ ] Frontend built and tested
- [ ] nginx configuration reviewed
- [ ] Firewall rules configured

### Deployment ✅
- [ ] Backend deployed
- [ ] Frontend deployed
- [ ] nginx configured with HTTPS
- [ ] Rate limiting verified
- [ ] Security headers verified
- [ ] Audit logging enabled

### Post-Deployment ✅
- [ ] Health checks passing
- [ ] SSO flow tested end-to-end
- [ ] Rate limiting tested
- [ ] Monitoring configured
- [ ] Alerts configured
- [ ] Documentation updated
- [ ] Team trained on operations

### Security ✅
- [ ] Signing secret rotated
- [ ] Direct backend access blocked
- [ ] SSL configuration verified (A+ rating)
- [ ] Security headers verified
- [ ] Audit logs reviewed
- [ ] Penetration testing completed

## Rollback Procedure

If issues arise:

```bash
# 1. Stop nginx
sudo systemctl stop nginx

# 2. Restore previous configuration
sudo cp /etc/nginx/sites-available/pulseql-ssl.conf.backup /etc/nginx/sites-available/pulseql-ssl.conf

# 3. Stop PulseQL
/opt/pulseql/stop-server.sh

# 4. Restore previous version
cp /opt/pulseql/cloudbeaver-web-server-previous.war /opt/pulseql/cloudbeaver-web-server.war

# 5. Restart services
/opt/pulseql/run-server.sh
sudo systemctl start nginx

# 6. Verify rollback
curl https://pulseql.example.com/health
```

## Troubleshooting

### SSO Validation Fails

**Check**:
1. Signing secret matches Pulsar
2. Token not expired
3. Issuer/audience correct
4. Backend logs for errors

```bash
tail -f /opt/pulseql/logs/cloudbeaver.log | grep SSO
```

### Rate Limiting Issues

**Check**:
```bash
# View rate limit stats
sudo grep "limiting requests" /var/log/nginx/pulseql_error.log | tail -20

# Adjust limits in nginx config if needed
sudo nano /etc/nginx/sites-available/pulseql-ssl.conf
sudo nginx -t && sudo systemctl reload nginx
```

### SSL Issues

**Check**:
```bash
# Test SSL configuration
openssl s_client -connect pulseql.example.com:443 -showcerts

# Check certificate expiration
echo | openssl s_client -connect pulseql.example.com:443 2>/dev/null | openssl x509 -noout -dates
```

## Maintenance

### Daily Tasks
- Review audit logs
- Check error rates
- Monitor blacklist size

### Weekly Tasks
- Review security alerts
- Check SSL certificate expiration
- Update dependencies

### Monthly Tasks
- Rotate signing secret
- Review access logs
- Security audit

## Support Contacts

- **Security Issues**: security@example.com
- **Deployment Issues**: devops@example.com
- **SSO Integration**: pulsar-team@example.com

---

**Document Version**: 1.0  
**Last Updated**: December 13, 2025  
**Review Required**: Before production deployment
