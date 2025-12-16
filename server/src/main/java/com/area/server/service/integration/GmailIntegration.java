package com.area.server.service.integration;

import com.area.server.model.ServiceConnection;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Gmail integration service providing email monitoring actions.
 * Implements the ServiceIntegration interface to enable auto-discovery.
 *
 * Gmail uses OAuth 2.0 authentication and provides actions (triggers)
 * for monitoring incoming emails with various filter criteria.
 */
@Service
public class GmailIntegration implements ServiceIntegration {

    @Override
    public ServiceConnection.ServiceType getType() {
        return ServiceConnection.ServiceType.GMAIL;
    }

    @Override
    public String getName() {
        return "Gmail";
    }

    @Override
    public String getDescription() {
        return "Monitor Gmail inbox for new emails with customizable filters";
    }

    @Override
    public List<ActionDefinition> getActions() {
        return List.of(
            new ActionDefinition(
                "gmail.new_unread_email",
                "New Unread Email",
                "Triggers when a new unread email is received in Gmail inbox",
                List.of(
                    new FieldDefinition(
                        "label",
                        "Label",
                        "string",
                        false,
                        "Filter emails by Gmail label (e.g., INBOX, IMPORTANT)"
                    ),
                    new FieldDefinition(
                        "subjectContains",
                        "Subject Contains",
                        "string",
                        false,
                        "Trigger only when email subject contains this text"
                    ),
                    new FieldDefinition(
                        "fromAddress",
                        "From Address",
                        "email",
                        false,
                        "Trigger only when email is from this sender address"
                    )
                )
            ),
            new ActionDefinition(
                "gmail.new_email_with_label",
                "New Email with Specific Label",
                "Triggers when a new email arrives with a specific Gmail label",
                List.of(
                    new FieldDefinition(
                        "label",
                        "Label",
                        "string",
                        true,
                        "Gmail label to monitor (required)"
                    ),
                    new FieldDefinition(
                        "fromAddress",
                        "From Address",
                        "email",
                        false,
                        "Optional: Filter by sender address"
                    )
                )
            )
        );
    }

    @Override
    public List<ReactionDefinition> getReactions() {
        // Gmail only provides actions (triggers), not reactions
        return Collections.emptyList();
    }

    @Override
    public boolean requiresAuthentication() {
        return true; // Gmail requires OAuth 2.0 authentication
    }
}
