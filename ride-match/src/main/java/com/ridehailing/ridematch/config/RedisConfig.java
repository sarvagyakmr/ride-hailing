package com.ridehailing.ridematch.config;

import com.ridehailing.ridematch.event.RideAssignmentSubscriber;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisServer;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${redis.embedded.enabled:false}")
    private boolean embeddedRedisEnabled;

    @Value("${redis.embedded.port:6379}")
    private int embeddedRedisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startEmbeddedRedis() {
        if (embeddedRedisEnabled) {
            try {
                redisServer = new RedisServer(embeddedRedisPort);
                redisServer.start();
                log.info("Embedded Redis started on port {}", embeddedRedisPort);
            } catch (Exception e) {
                log.error("Failed to start embedded Redis", e);
            }
        }
    }

    @PreDestroy
    public void stopEmbeddedRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            log.info("Embedded Redis stopped");
        }
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ChannelTopic rideRequestedTopic() {
        return new ChannelTopic("ride:requested");
    }

    @Bean(name = "driverRideOfferTopic")
    public ChannelTopic driverRideOfferTopic() {
        return new ChannelTopic("driver:ride:offer");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter rideAssignmentListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer() {
            @Override
            public boolean isAutoStartup() {
                return embeddedRedisEnabled;
            }
        };
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(rideAssignmentListener, rideRequestedTopic());
        return container;
    }

    @Bean
    public MessageListenerAdapter rideAssignmentListener(RideAssignmentSubscriber rideAssignmentSubscriber) {
        return new MessageListenerAdapter(rideAssignmentSubscriber, "onMessage");
    }
}
