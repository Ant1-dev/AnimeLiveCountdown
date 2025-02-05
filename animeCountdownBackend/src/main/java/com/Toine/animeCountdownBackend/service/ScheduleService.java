package com.Toine.animeCountdownBackend.service;

import org.springframework.graphql.client.GraphQlClient;
import org.springframework.stereotype.Service;

@Service
public class ScheduleService {

    private final GraphQlClient graphQlClient;

    public ScheduleService(GraphQlClient graphQlClient) {
        this.graphQlClient = graphQlClient;
    }
}
