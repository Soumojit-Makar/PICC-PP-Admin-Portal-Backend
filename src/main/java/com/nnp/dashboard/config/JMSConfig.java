package com.nnp.dashboard.config;


import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * Spring JMS configuration for the ActiveMQ Artemis broker.
 *
 * Provides:
 *   - an {@link ActiveMQConnectionFactory} wired from the config-server values
 *     ({@code spring.activemq.*})
 *   - a connection-pooling wrapper and a {@link JmsTemplate} for sending
 *   - a listener container factory plus a JSON {@code MessageConverter} so
 *     producers and consumers exchange objects as TEXT/JSON messages.
 *
 * The defined queue {@value REQUEST_QUEUE} is where
 * {@link com.nnp.dashboard.event.listener.LatchEventListener#doFinalActivity}
 * sends the environment-replication hand-off message.
 */
@EnableJms
@Configuration	
public class JMSConfig {
	
	@Value("${spring.activemq.broker-url:tcp://localhost:61616}")
	private String brokerUrl;
	
	@Value("${spring.activemq.user:artemis}")
	private String user;
	
	@Value("${spring.activemq.password:artemis}")
	private String password;
	
	public static final String REQUEST_QUEUE = 	"env-rep::env_replication"; //"lcnc::lcnc-queue";
	
	
	@Bean
	public ActiveMQConnectionFactory senderActiveMQConnectionFactory() {
		return new ActiveMQConnectionFactory(brokerUrl,user,password);
	}
	
	@Bean
	public CachingConnectionFactory cachingConnectionFactory() {
		return new CachingConnectionFactory(senderActiveMQConnectionFactory());
	}

	@Bean
	public JmsTemplate jmsTemplate() {
		return new JmsTemplate(cachingConnectionFactory());
	}
	
	@Bean
    public JmsListenerContainerFactory<?> queueListenerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setMessageConverter(messageConverter());
        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }


}
