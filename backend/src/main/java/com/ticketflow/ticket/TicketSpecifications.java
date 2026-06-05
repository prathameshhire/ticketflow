package com.ticketflow.ticket;

import org.springframework.data.jpa.domain.Specification;

import com.ticketflow.auth.UserPrincipal;
import com.ticketflow.user.UserRole;

import jakarta.persistence.criteria.JoinType;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> visibleTo(UserPrincipal principal) {
        return (root, query, criteriaBuilder) -> {
            if (principal.role() == UserRole.ADMIN) {
                return criteriaBuilder.conjunction();
            }

            if (principal.role() == UserRole.CUSTOMER) {
                return criteriaBuilder.equal(root.get("customer").get("id"), principal.id());
            }

            return criteriaBuilder.equal(root.get("assignedAgent").get("id"), principal.id());
        };
    }

    public static Specification<Ticket> statusEquals(TicketStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Ticket> priorityEquals(TicketPriority priority) {
        return (root, query, criteriaBuilder) ->
                priority == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> assignedAgentEquals(Long assignedAgentId) {
        return (root, query, criteriaBuilder) -> assignedAgentId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("assignedAgent").get("id"), assignedAgentId);
    }

    public static Specification<Ticket> customerEquals(Long customerId) {
        return (root, query, criteriaBuilder) -> customerId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Ticket> search(String queryText) {
        return (root, query, criteriaBuilder) -> {
            if (queryText == null || queryText.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + queryText.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Ticket> withUsers() {
        return (root, query, criteriaBuilder) -> {
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("customer", JoinType.LEFT);
                root.fetch("assignedAgent", JoinType.LEFT);
                query.distinct(true);
            }
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<Ticket> orderBy(TicketSortField sortField) {
        return (root, query, criteriaBuilder) -> {
            if (query == null || Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType())) {
                return criteriaBuilder.conjunction();
            }

            if (sortField == TicketSortField.PRIORITY) {
                var priorityRank = criteriaBuilder.<Integer>selectCase()
                        .when(criteriaBuilder.equal(root.get("priority"), TicketPriority.URGENT), 4)
                        .when(criteriaBuilder.equal(root.get("priority"), TicketPriority.HIGH), 3)
                        .when(criteriaBuilder.equal(root.get("priority"), TicketPriority.MEDIUM), 2)
                        .otherwise(1);
                query.orderBy(criteriaBuilder.desc(priorityRank), criteriaBuilder.desc(root.get("createdAt")));
            } else {
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            }

            return criteriaBuilder.conjunction();
        };
    }
}
