package com.planify.notification.config;

import com.planify.notification.event.InvitationRespondedEvent;
import com.planify.notification.event.InvitationSentEvent;
import com.planify.notification.event.JoinRequestRespondedEvent;
import com.planify.notification.event.JoinRequestsSentEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    /**
     * ContainerFactory za join-request-responded topic, desirializira vrednosti v objekt tipa JoinRequestRespondedEvent.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JoinRequestRespondedEvent> joinRequestsRespondedKafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> defaultConsumerFactory) {
        Map<String, Object> props = new HashMap<>(((DefaultKafkaConsumerFactory<?, ?>) defaultConsumerFactory).getConfigurationProperties());
        stripJsonDeserializerProps(props);

        JsonDeserializer<JoinRequestRespondedEvent> valueDeserializer = new JsonDeserializer<>(JoinRequestRespondedEvent.class);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeMapperForKey(false);
        valueDeserializer.setUseTypeHeaders(false);

        DefaultKafkaConsumerFactory<String, JoinRequestRespondedEvent> cf = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), valueDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, JoinRequestRespondedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        return factory;
    }

    /**
     * ContainerFactory za join-request-sent topic, deserializira vrednosti v objekt tipa JoinRequestsSentEvent.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JoinRequestsSentEvent> joinRequestsSentKafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> defaultConsumerFactory) {
        Map<String, Object> props = new HashMap<>(((DefaultKafkaConsumerFactory<?, ?>) defaultConsumerFactory).getConfigurationProperties());
        stripJsonDeserializerProps(props);

        JsonDeserializer<JoinRequestsSentEvent> valueDeserializer = new JsonDeserializer<>(JoinRequestsSentEvent.class);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeMapperForKey(false);
        valueDeserializer.setUseTypeHeaders(false);

        DefaultKafkaConsumerFactory<String, JoinRequestsSentEvent> cf = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), valueDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, JoinRequestsSentEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        return factory;
    }

    /**
     * ContainerFactory za invitation-sent topic, deserializira vrednosti v objekt tipa InvitationSentEvent.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InvitationSentEvent> invitationsSentKafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> defaultConsumerFactory) {
        Map<String, Object> props = new HashMap<>(((DefaultKafkaConsumerFactory<?, ?>) defaultConsumerFactory).getConfigurationProperties());
        stripJsonDeserializerProps(props);

        JsonDeserializer<InvitationSentEvent> valueDeserializer = new JsonDeserializer<>(InvitationSentEvent.class);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeMapperForKey(false);
        valueDeserializer.setUseTypeHeaders(false);

        DefaultKafkaConsumerFactory<String, InvitationSentEvent> cf = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), valueDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, InvitationSentEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        return factory;
    }

    /**
     * ContainerFactory za invitation-responded topic, deserializira vrednosti v objekt tipa InvitationRespondedEvent.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InvitationRespondedEvent> invitationsRespondedKafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> defaultConsumerFactory) {
        Map<String, Object> props = new HashMap<>(((DefaultKafkaConsumerFactory<?, ?>) defaultConsumerFactory).getConfigurationProperties());
        stripJsonDeserializerProps(props);

        JsonDeserializer<InvitationRespondedEvent> valueDeserializer = new JsonDeserializer<>(InvitationRespondedEvent.class);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeMapperForKey(false);
        valueDeserializer.setUseTypeHeaders(false);

        DefaultKafkaConsumerFactory<String, InvitationRespondedEvent> cf = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), valueDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, InvitationRespondedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        return factory;
    }

    private void stripJsonDeserializerProps(Map<String, Object> props) {
        props.remove("spring.json.trusted.packages");
        props.remove("spring.json.use.type.headers");
        props.remove("spring.json.value.default.type");
        props.remove("spring.json.type.mapping");
        props.remove("spring.deserializer.value.delegate.class");
        props.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
    }
}
