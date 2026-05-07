package com.hasith.auction.beans;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/auctionTopic")
})
public class NotificationBean implements MessageListener {

    @Override
    public void onMessage(Message message) {
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            try {
                String bidUpdate = textMessage.getText();
                System.out.println(" [NotificationBean] New bid message received: " + bidUpdate);
            } catch (JMSException e) {
                System.err.println(" [NotificationBean] Failed to read message:");
                e.printStackTrace();
            }
        } else {
            System.err.println(" [NotificationBean] Unsupported message type: " + message.getClass().getName());
        }
    }
}
