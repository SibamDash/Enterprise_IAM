package com.enterprise.iam.security;

import com.enterprise.iam.domain.Policy;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyEngine {

    private final PolicyRepository policyRepository;
    private final ExpressionParser parser = new SpelExpressionParser();

    public boolean evaluate(User user, String action, String resourceName, Map<String, Object> resourceAttributes) {
        if (user == null || user.getOrganizationId() == null) {
            return false; // Cannot evaluate without a user and tenant
        }

        List<Policy> policies = policyRepository.findAllByOrganizationIdOrderByPriorityDesc(user.getOrganizationId());

        boolean hasAllow = false;

        for (Policy policy : policies) {
            if (matchesAction(policy.getActions(), action) && matchesResource(policy.getResources(), resourceName)) {
                
                boolean conditionMet = evaluateCondition(policy.getConditions(), user, resourceAttributes);
                
                if (conditionMet) {
                    if ("DENY".equalsIgnoreCase(policy.getEffect())) {
                        return false; // Explicit DENY immediately overrides any ALLOW
                    }
                    if ("ALLOW".equalsIgnoreCase(policy.getEffect())) {
                        hasAllow = true;
                    }
                }
            }
        }

        return hasAllow; // If no explicit DENY was hit, return true if any ALLOW matched.
    }

    private boolean matchesAction(List<String> policyActions, String action) {
        if (policyActions == null || policyActions.isEmpty()) return false;
        if (policyActions.contains("*")) return true;
        return policyActions.contains(action);
    }

    private boolean matchesResource(List<String> policyResources, String resourceName) {
        if (policyResources == null || policyResources.isEmpty()) return false;
        if (policyResources.contains("*")) return true;
        
        for (String pattern : policyResources) {
            if (pattern.endsWith("*")) {
                String prefix = pattern.substring(0, pattern.length() - 1);
                if (resourceName.startsWith(prefix)) return true;
            } else if (pattern.equals(resourceName)) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateCondition(String conditionExpression, User user, Map<String, Object> resourceAttributes) {
        if (!StringUtils.hasText(conditionExpression)) {
            return true; // No condition means unconditional match
        }
        
        try {
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("user", user);
            context.setVariable("resource", resourceAttributes);
            
            Expression expression = parser.parseExpression(conditionExpression);
            Boolean result = expression.getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception ex) {
            // Log warning about invalid expression, evaluate to false for safety
            return false;
        }
    }
}
