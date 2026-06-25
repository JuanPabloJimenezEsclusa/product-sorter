REGION ?= eu-west-1

.PHONY: deploy destroy token test info help

deploy:
	cd cloud-formation && ./start.sh

deploy-full:
	cd cloud-formation && ./start.sh buildImage

destroy:
	cd cloud-formation && ./stop.sh

destroy-full:
	cd cloud-formation && ./stop.sh removeImages

token:
	./get-token.sh

test:
	@AlbDnsName=$$(aws cloudformation describe-stacks \
	  --stack-name product-sorter-app --region $(REGION) \
	  --query "Stacks[0].Outputs[?OutputKey=='AlbDnsName'].OutputValue" \
	  --output text 2>/dev/null); \
	if [ -n "$$AlbDnsName" ]; then \
	  curl -sk "https://$$AlbDnsName/actuator/health" | jq; \
	else \
	  echo "Stack not deployed"; \
	fi

info:
	@for stack in product-sorter-cognito product-sorter-secrets product-sorter-app; do \
	  echo "=== $$stack ==="; \
	  aws cloudformation describe-stacks \
	    --stack-name "$$stack" --region $(REGION) \
	    --query 'Stacks[0].Outputs' --output table 2>/dev/null || echo "  (not found)"; \
	  echo ""; \
	done

help:
	@echo "make deploy           Deploy (reuse existing ECR image)"
	@echo "make deploy-full      Deploy + build + push new image"
	@echo "make destroy          Tear down stacks + ECR + CNAMEs"
	@echo "make destroy-full     destroy + remove local Docker images"
	@echo "make token            Get Cognito access token"
	@echo "make test             Smoke test /actuator/health"
	@echo "make info             Show all stack outputs"
