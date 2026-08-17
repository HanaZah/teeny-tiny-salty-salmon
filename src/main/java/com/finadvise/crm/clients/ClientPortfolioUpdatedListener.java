package com.finadvise.crm.clients;

import com.finadvise.crm.common.ClientPortfolioUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ClientPortfolioUpdatedListener {
    private final ClientRepository clientRepository;
    private final Clock clock;

    @EventListener
    public void handleClientPortfolioUpdate(ClientPortfolioUpdatedEvent event) {
        clientRepository.touchClientVersionAndLastUpdate(event.clientUid(), LocalDate.now(clock));
    }
}
