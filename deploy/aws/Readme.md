# Product Sorter — AWS

---

## Prerequisites

- **AWS CLI** v2+ with configured credentials (`aws sts get-caller-identity` succeeds)
- **Docker** with daemon running
- **OpenSSL** for `get-token.sh`
- **Route53 hosted zone** already created in eu-west-1
- **Domain name** registered, pointing to Route53
- **MongoDB Atlas** (or compatible) connection string with `authSource=admin`
- AWS IAM permissions: CloudFormation, ECS, ECR, IAM, Route53, ACM, VPC, Secrets Manager, Cognito

### First-time setup

1. Create Route53 hosted zone for your domain
2. `cp .env.example .env` and fill in real values
3. Verify `aws sts get-caller-identity` returns your AWS account

---

## Stacks

| Stack | Template | Resources |
|-------|----------|-----------|
| `product-sorter-cognito` | `01-cognito.yml` | UserPool, UserPoolClient → JwksUri |
| `product-sorter-secrets` | `02-secrets.yml` | Secrets Manager: MongoDB URI, Cognito client secret |
| `product-sorter-app` | `03-app.yml` | VPC (public/private subnets, IGW, NAT), ECS Fargate, ALB, ACM, Route53, CloudWatch |

---

## Deploy

### .env

Create/Edit `deploy/aws/.env`:

```bash
PRODUCT_SORTER_USERNAME=""
PRODUCT_SORTER_PASSWORD=""
MONGODB_URI=""
DOMAIN_NAME=""
```

### Quick start

```bash
cd deploy/aws

# First deploy: build Docker image + push to ECR + deploy 3 stacks
./cloud-formation/start.sh buildImage

# Incremental deploy
ECR_IMAGE_TAG=1.0.1 ./cloud-formation/start.sh

# or
make deploy-full
```

### Tear down

```bash
./cloud-formation/stop.sh removeImages

# or
make destroy-full
```

---

## Links

- [CloudFormation](https://eu-west-1.console.aws.amazon.com/cloudformation/home?region=eu-west-1)
- [Cognito](https://eu-west-1.console.aws.amazon.com/cognito/v2/idp/user-pools?region=eu-west-1)
- [Secrets Manager](https://eu-west-1.console.aws.amazon.com/secretsmanager/home?region=eu-west-1) 💰
- [VPC](https://eu-west-1.console.aws.amazon.com/vpcconsole/home?region=eu-west-1#vpcs)
- [Elastic IP](https://eu-west-1.console.aws.amazon.com/vpcconsole/home?region=eu-west-1#Addresses)
- [NAT Gateway](https://eu-west-1.console.aws.amazon.com/vpcconsole/home?region=eu-west-1#NatGateways) 💰💰💰
- [ECR](https://eu-west-1.console.aws.amazon.com/ecr/repositories/private/546053716955/product-sorter)
- [ECS](https://eu-west-1.console.aws.amazon.com/ecs/v2/clusters/product-sorter/services?region=eu-west-1) 💰
- [ACM](https://eu-west-1.console.aws.amazon.com/acm/home?region=eu-west-1#/certificates/list)
- [ALB](https://eu-west-1.console.aws.amazon.com/ec2/home?region=eu-west-1#LoadBalancers) 💰💰💰
- [Route53](https://eu-west-1.console.aws.amazon.com/route53/v2/hostedzones) 💰
- [CloudWatch](https://eu-west-1.console.aws.amazon.com/cloudwatch/home?region=eu-west-1#logsV2:log-groups) 💰

---

## Monthly cost estimate

Pricing eu-west-1 (Ireland), FARGATE_SPOT, DesiredCount=1. All prices in USD.

| Resource | Unit | Qty | Unit price | Monthly |
|----------|------|:---:|-----------:|------:|
| NAT Gateway | Hour | 730 | $0.048 | $35.04 |
| ALB | Hour | 730 | $0.0252 | $18.40 |
| ALB LCU (min load) | LCU-hour | 730 | $0.0084 | $6.13 |
| ECS Fargate Spot 1 vCPU | vCPU-hour | 730 | $0.00395 | $2.88 |
| ECS Fargate Spot 2 GB RAM | GB-hour | 1460 | $0.00132 | $1.93 |
| Route53 Hosted Zone | Month | 1 | $0.50 | $0.50 |
| Secrets Manager (1 secret) | Month | 1 | $0.40 | $0.40 |
| CloudWatch Logs (~5 GB) | GB ingested | 5 | $0.76 | $3.80 |
| NAT Data Transfer (~10 GB) | GB processed | 10 | $0.048 | $0.48 |
| Cognito (MAU < 50k) | Month | 1 | $0.00 | $0.00 |
| ACM Certificate | Month | 1 | $0.00 | $0.00 |
| Elastic IP (attached) | Month | 1 | $0.00 | $0.00 |
| | | | **Total** | **~$70** |

> ECS uses FARGATE_SPOT — compute cost drops ~90% vs. On-Demand Fargate. \
> All prices are estimates and do not include data transfer for high-traffic scenarios.
