/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.md file.
 */

package org.mule.module.magento.api.order;

import com.magento.api.OrderItemIdQty;
import com.magento.api.SalesOrderEntity;
import com.magento.api.SalesOrderEntityToReorder;
import com.magento.api.SalesOrderEntityToUpdate;
import com.magento.api.SalesOrderInvoiceEntity;
import com.magento.api.SalesOrderListEntity;
import com.magento.api.SalesOrderShipmentEntity;

import org.mule.module.magento.api.order.model.Carrier;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

/**
 * Facade for the Magento Sales Order API. This Facade uses order, shipments and
 * invoices ids called, {@code orderId}, {@code shipmentId} and {@code invoiceId}
 * respectively. However, those Ids correspond internally to the Magento's {@code
 * xxxIncrementId}. Actual {@code xxxId} are used internally by magento and not
 * exposed by the webservice in most operations
 * 
 * @author flbulgarelli
 * @param <ExceptionType> the type of exception that this client throws
 */
public interface MagentoOrderClient<ExceptionType extends Exception>
{

    /**
     * Lists order attributes that match the 
     * given filtering expression
     * 
     * @return a list of order attributes
     */
    @NotNull
    List<SalesOrderListEntity> listOrders(String filter) throws ExceptionType;

    /**
     * Answers the order properties for the given orderId
     * 
     * @param orderId
     * @return the order properties
     */
    @NotNull
    com.magento.api.SalesOrderEntity getOrder(@NotNull String orderId) throws ExceptionType;

    /**
     * Puts order on hold
     *
     */
    boolean holdOrder(@NotNull String orderId) throws ExceptionType;

    /**
     * Releases order
     *
     */
    boolean unholdOrder(String orderId) throws ExceptionType;

    /**
     * Cancels an order
     *
     * @param orderId the order to cancel
     */
    boolean cancelOrder(@NotNull String orderId) throws ExceptionType;

    /**
     * Adds a comment to the given order id
     *
     * @param orderId the order id
     * @param status
     * @param comment
     * @param sendEmail if an email must be sent after shipment creation
     */
    boolean addOrderComment(@NotNull String orderId,
                            @NotNull String status,
                            @NotNull String comment,
                            boolean sendEmail) throws ExceptionType;

    /**
     * Lists order shipment atrributes that match the given 
     * optional filtering expression
     * 
     * @return list of string-object map order shipments attributes
     */
    @NotNull
    List<SalesOrderShipmentEntity> listOrdersShipments(String filter) throws ExceptionType;

    /**
     * Retrieves order shipment information
     * 
     * @return sales order shipment information
     */
    @NotNull
    SalesOrderShipmentEntity getOrderShipment(String shipmentId) throws ExceptionType;

    /**
     * Adds a comment to the shipment
     *
     * @param shipmentId the shipment's increment id
     * @param comment the comment to add
     * @param sendEmail if an email must be sent after shipment creation
     * @param includeCommentInEmail if the comment must be sent in the email
     */
    boolean addOrderShipmentComment(@NotNull String shipmentId,
                                    @NotNull String comment,
                                    boolean sendEmail,
                                    boolean includeCommentInEmail) throws ExceptionType;

    /**
     * Returns a list of carriers for the given order id
     * 
     * @param order id
     * @return list of carriers
     */
    @NotNull
    List<Carrier> getOrderShipmentCarriers(@NotNull String orderId) throws ExceptionType;

    /**
     * Adds a new tracking number
     * 
     * @param shipmentId the shipment id
     * @param carrierCode the new track's carrier code
     * @param title the new track's title
     * @param trackNumber the new track's number
     * @return the new track's id
     */
    int addOrderShipmentTrack(@NotNull String shipmentId,
                              @NotNull String carrierCode,
                              @NotNull String title,
                              @NotNull String trackNumber) throws ExceptionType;

    /**
     * Deletes the given track of the given order's shipment
     * 
     * @param shipmentId the target shipment id
     * @param trackId the id of the track to delete
     * @return if the tracking number is removed from the shipment
     */
    boolean deleteOrderShipmentTrack(@NotNull String shipmentId, @NotNull String trackId) throws ExceptionType;

    /**
     * Creates a shipment for order
     * 
     * @param orderId the order increment id
     * @param itemsQuantities a map containing an entry per each (orderItemId,
     *            quantity) pair
     * @param comment an optional comment
     * @param sendEmail if an email must be sent after shipment creation
     * @param includeCommentInEmail if the comment must be sent in the email
     * @return the new shipment's id
     */
    String createOrderShipment(@NotNull String orderId,
                               @NotNull List<OrderItemIdQty> itemsQuantities,
                               String comment,
                               boolean sendEmail,
                               boolean includeCommentInEmail) throws ExceptionType;

    /**
     * Lists order invoices that match the given filtering expression
     * 
     * @return list of string-object maps order attributes
     */
    @NotNull
    List<SalesOrderInvoiceEntity> listOrdersInvoices(String filter) throws ExceptionType;

    /**
     * Retrieves order invoice information
     * 
     *
     * @param invoiceId
     * @return the invoice attributes
     */
    @NotNull
    SalesOrderInvoiceEntity getOrderInvoice(@NotNull String invoiceId) throws ExceptionType;

    /**
     * Creates an invoice for the given order
     * 
     * @param orderId
     * @param itemsQuantities a map containing an entry per each (orderItemId,
     *            quantity) pair
     * @param comment an optional comment
     * @param sendEmail if an email must be sent after shipment creation
     * @param includeCommentInEmail if the comment must be sent in the email
     * @return the new invoice's id
     */
    String createOrderInvoice(@NotNull String orderId,
                              @NotNull List<OrderItemIdQty> itemsQuantities,
                              String comment,
                              boolean sendEmail,
                              boolean includeCommentInEmail) throws ExceptionType;

    /**
     * Adds a comment to the given order's invoice
     *
     * @param invoiceId the invoice id
     * @param comment the comment to add
     * @param sendEmail if an email must be sent after shipment creation
     * @param includeCommentInEmail if the comment must be sent in the email
     */
    boolean addOrderInvoiceComment(@NotNull String invoiceId,
                                   @NotNull String comment,
                                   boolean sendEmail,
                                   boolean includeCommentInEmail) throws ExceptionType;

    /**
     * Captures and invoice
     * 
     * @param invoiceId the invoice to capture
     */
    void captureOrderInvoice(@NotNull String invoiceId) throws ExceptionType;

    /**
     * Voids an invoice
     *
     * @param invoiceId the invoice id
     */
    boolean voidOrderInvoice(@NotNull String invoiceId) throws ExceptionType;

    /**
     * Cancels an order's invoice
     * 
     * @param invoiceId the invoice id
     */
    void cancelOrderInvoice(@NotNull String invoiceId) throws ExceptionType;

    /**
     * Creates an order based on with incrementId and cancel parent order
     * 
     * @param incrementId the increment id of parent order
     * @param customerData the data of new order
     */
    void reorderOrder(@NotNull String incrementId, SalesOrderEntityToReorder customerData) throws ExceptionType;
    
    /**
     * Update status of order based on with incrementId
     * 
     * @param incrementId the increment id of parent order
     * @param orderData the data of new order
     */
    void updateOrder(@NotNull String incrementId, SalesOrderEntityToUpdate orderData) throws ExceptionType;
}
