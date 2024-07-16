package org.example.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.AbstractJackson2MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.example.RabbitQueue.*;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitConfiguration {

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public Queue textMessageQueue() {
        return new Queue(TEXT_MESSAGE_UPDATE);
    }

    @Bean
    public Queue photoMessageQueue() {
        return new Queue(PHOTO_MESSAGE_UPDATE);
    }

    @Bean
    public Queue answerMessageQueue() {
        return new Queue(ANSWER_MESSAGE);
    }
    @Bean
    public Queue contactMessageQueue() {
        return new Queue(CONTACT_UPDATE);
    }

    @Bean
    public Queue callBackDataQueue() {
        return new Queue(CALLBACK_UPDATE);
    }
    @Bean
    public Queue newTicketsRequest() {
        return new Queue(NEW_TICKET_REQUEST);
    }
    @Bean
    public Queue answerPhotoQueue() { return new Queue(ANSWER_PHOTO); }
    @Bean
    public Queue answerDocQueue() { return new Queue(ANSWER_DOC); }
}
