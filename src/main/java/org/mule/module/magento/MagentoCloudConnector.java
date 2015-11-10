/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.md file.
 */

package org.mule.module.magento;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Priority;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.MetaDataKeyRetriever;
import org.mule.api.annotations.MetaDataRetriever;
import org.mule.api.annotations.MetaDataSwitch;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.QueryTranslator;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.Payload;
import org.mule.common.metadata.DefaultMetaData;
import org.mule.common.metadata.DefaultMetaDataKey;
import org.mule.common.metadata.DefaultPojoMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.common.query.Query;
import org.mule.module.magento.api.AxisPortProvider;
import org.mule.module.magento.api.DefaultAxisPortProvider;
import org.mule.module.magento.api.MagentoClientAdaptor;
import org.mule.module.magento.api.MagentoException;
import org.mule.module.magento.api.catalog.AxisMagentoCatalogClient;
import org.mule.module.magento.api.catalog.MagentoCatalogClient;
import org.mule.module.magento.api.catalog.model.MediaMimeType;
import org.mule.module.magento.api.catalog.model.ProductIdentifiers;
import org.mule.module.magento.api.customer.AxisMagentoInventoryClient;
import org.mule.module.magento.api.customer.MagentoInventoryClient;
import org.mule.module.magento.api.directory.AxisMagentoDirectoryClient;
import org.mule.module.magento.api.directory.MagentoDirectoryClient;
import org.mule.module.magento.api.inventory.AxisMagentoCustomerClient;
import org.mule.module.magento.api.inventory.MagentoCustomerClient;
import org.mule.module.magento.api.order.AxisMagentoOrderClient;
import org.mule.module.magento.api.order.MagentoOrderClient;
import org.mule.module.magento.api.order.model.Carrier;
import org.mule.module.magento.api.shoppingCart.AxisMagentoShoppingCartClient;
import org.mule.module.magento.api.shoppingCart.MagentoShoppingCartClient;
import org.mule.module.magento.query.MagentoQueryVisitor;

import com.magento.api.*;

//import java.util.Map;

/**
 * Magento is a feature-rich eCommerce platform built on open-source technology that provides online merchants with
 * great and control over the look, content and functionality of their eCommerce store.
 *
 * @author MuleSoft, Inc.
 */
@Connector(name = "magento", schemaVersion = "1.1", friendlyName = "Magento", minMuleVersion = "3.5", metaData=MetaDataSwitch.ON)
public class MagentoCloudConnector {

    private static final String ENTITY_PACKAGE = "com.magento.api";

    private MagentoOrderClient<MagentoException> orderClient;
    private MagentoCustomerClient<MagentoException> customerClient;
    private MagentoInventoryClient<MagentoException> inventoryClient;
    private MagentoDirectoryClient<MagentoException> directoryClient;
    private MagentoCatalogClient<MagentoException> catalogClient;
    private MagentoShoppingCartClient<MagentoException> shoppingCartClient;

    /**
     *
     */
    private String user;

    /**
     * The address to access Magento Web Services
     */
    private String serverAddress;

    /**
     *
     * @param username The user name to access Magento Web Services
     * @param password The password to access Magento Web Services
     * @param address The address to access Magento Web Services
     */
    @Connect
    public void initialiseConnector(@ConnectionKey String username, @Password String password, String address)
            throws ConnectionException {
        setUser(username);
        setServerAddress(address);
        PortProviderInitializer initializer = new PortProviderInitializer(username, password, address);
        setOrderClient(new AxisMagentoOrderClient(initializer.getPortProvider()));
        setCustomerClient(new AxisMagentoCustomerClient(initializer.getPortProvider()));
        setInventoryClient(new AxisMagentoInventoryClient(initializer.getPortProvider()));
        setDirectoryClient(new AxisMagentoDirectoryClient(initializer.getPortProvider()));
        setCatalogClient(new AxisMagentoCatalogClient(initializer.getPortProvider()));
        setShoppingCartClient(new AxisMagentoShoppingCartClient(initializer.getPortProvider()));

        //Dummy operation for connectivity testing
        try {
            getCatalogCurrentStoreView();
        } catch (Exception e) {
            final ConnectionException connectionException = new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, null, e.getMessage());
            connectionException.initCause(e);
            throw connectionException;

        }
    }

    @ValidateConnection
    public boolean validateConnection() {
        return orderClient != null && customerClient != null && inventoryClient != null && directoryClient != null &&
                catalogClient != null && shoppingCartClient != null;
    }

    @Disconnect
    public void disconnect() {
        setOrderClient(null);
        setCustomerClient(null);
        setInventoryClient(null);
        setDirectoryClient(null);
        setCatalogClient(null);
        setShoppingCartClient(null);
    }

    @ConnectionIdentifier
    public String connectionId() {
        return String.format("%s-at-%s", getUser(), getServerAddress());
    }

    private MetaDataKey createKey(Class<?> cls) {
        return new DefaultMetaDataKey(cls.getSimpleName(), cls.getSimpleName());
    }

    @MetaDataKeyRetriever
    public List<MetaDataKey> getMetadataKeys() {
        List<MetaDataKey> keys = new ArrayList<MetaDataKey>();
        keys.add(this.createKey(ApiEntity.class));
        keys.add(this.createKey(ApiMethodEntity.class));
        keys.add(this.createKey(AssociativeEntity.class));
        keys.add(this.createKey(AssociativeMultiEntity.class));
        keys.add(this.createKey(CatalogAttributeEntity.class));
        keys.add(this.createKey(CatalogAttributeOptionEntity.class));
        keys.add(this.createKey(CatalogCategoryEntity.class));
        keys.add(this.createKey(CatalogInventoryStockItemEntity.class));
        keys.add(this.createKey(CatalogInventoryStockItemUpdateEntity.class));
        keys.add(this.createKey(CatalogProductAdditionalAttributesEntity.class));
        keys.add(this.createKey(CatalogProductAttributeEntity.class));
        keys.add(this.createKey(CatalogProductAttributeFrontendLabelEntity.class));
        keys.add(this.createKey(CatalogProductAttributeMediaCreateEntity.class));
        keys.add(this.createKey(CatalogProductAttributeMediaTypeEntity.class));
        keys.add(this.createKey(CatalogProductAttributeOptionLabelEntity.class));
        keys.add(this.createKey(CatalogProductAttributeSetEntity.class));
        keys.add(this.createKey(CatalogProductCreateEntity.class));
        keys.add(this.createKey(CatalogProductCustomOptionAdditionalFieldsEntity.class));
        keys.add(this.createKey(CatalogProductCustomOptionInfoEntity.class));
        keys.add(this.createKey(CatalogProductCustomOptionListEntity.class));
        keys.add(this.createKey(CatalogProductCustomOptionTypesEntity.class));
        keys.add(this.createKey(CatalogProductCustomOptionValueAddEntity.class));
        keys.add(this.createKey(CatalogProductCustomOptionValueInfoEntity.class));
        keys.add(this.createKey(CatalogProductCustomOptionValueListEntity.class));
        keys.add(this.createKey(CatalogProductCustomOptionValueUpdateEntity.class));
        keys.add(this.createKey(CatalogProductDownloadableLinkAddEntity.class));
        keys.add(this.createKey(CatalogProductDownloadableLinkAddSampleEntity.class));
        keys.add(this.createKey(CatalogProductDownloadableLinkEntity.class));
        keys.add(this.createKey(CatalogProductDownloadableLinkFileEntity.class));
        keys.add(this.createKey(CatalogProductDownloadableLinkFileInfoEntity.class));
        keys.add(this.createKey(CatalogProductDownloadableLinkInfoEntity.class));
        keys.add(this.createKey(CatalogProductDownloadableLinkSampleEntity.class));
        keys.add(this.createKey(CatalogProductEntity.class));
        keys.add(this.createKey(CatalogProductImageEntity.class));
        keys.add(this.createKey(CatalogProductImageFileEntity.class));
        keys.add(this.createKey(CatalogProductLinkAttributeEntity.class));
        keys.add(this.createKey(CatalogProductLinkEntity.class));
        keys.add(this.createKey(CatalogProductReturnEntity.class));
        keys.add(this.createKey(CatalogProductTagAddEntity.class));
        keys.add(this.createKey(CatalogProductTagInfoEntity.class));
        keys.add(this.createKey(CatalogProductTagListEntity.class));
        keys.add(this.createKey(CatalogProductTagUpdateEntity.class));
        keys.add(this.createKey(CatalogProductTierPriceEntity.class));
        keys.add(this.createKey(CatalogProductTypeEntity.class));
        keys.add(this.createKey(CustomerCustomerEntity.class));
        keys.add(this.createKey(CustomerGroupEntity.class));
        keys.add(this.createKey(DirectoryCountryEntity.class));
        keys.add(this.createKey(DirectoryRegionEntity.class));
        keys.add(this.createKey(ExistsFaltureEntity.class));
        keys.add(this.createKey(GiftMessageAssociativeProductsEntity.class));
        keys.add(this.createKey(MagentoInfoEntity.class));
        keys.add(this.createKey(SalesOrderAddressEntity.class));
        keys.add(this.createKey(SalesOrderCreditmemoCommentEntity.class));
        keys.add(this.createKey(SalesOrderCreditmemoEntity.class));
        keys.add(this.createKey(SalesOrderCreditmemoItemEntity.class));
        keys.add(this.createKey(SalesOrderEntity.class));
        keys.add(this.createKey(SalesOrderEntityToReorder.class));
        keys.add(this.createKey(SalesOrderInvoiceCommentEntity.class));
        keys.add(this.createKey(SalesOrderInvoiceEntity.class));
        keys.add(this.createKey(SalesOrderInvoiceItemEntity.class));
        keys.add(this.createKey(SalesOrderItemEntity.class));
        keys.add(this.createKey(SalesOrderListEntity.class));
        keys.add(this.createKey(SalesOrderPaymentEntity.class));
        keys.add(this.createKey(SalesOrderShipmentCommentEntity.class));
        keys.add(this.createKey(SalesOrderShipmentEntity.class));
        keys.add(this.createKey(SalesOrderShipmentItemEntity.class));
        keys.add(this.createKey(SalesOrderShipmentTrackEntity.class));
        keys.add(this.createKey(SalesOrderStatusHistoryEntity.class));
        keys.add(this.createKey(ShoppingCartAddressEntity.class));
        keys.add(this.createKey(ShoppingCartCustomerAddressEntity.class));
        keys.add(this.createKey(ShoppingCartCustomerEntity.class));
        keys.add(this.createKey(ShoppingCartInfoEntity.class));
        keys.add(this.createKey(ShoppingCartItemEntity.class));
        keys.add(this.createKey(ShoppingCartLicenseEntity.class));
        keys.add(this.createKey(ShoppingCartPaymentEntity.class));
        keys.add(this.createKey(ShoppingCartPaymentMethodEntity.class));
        keys.add(this.createKey(ShoppingCartProductEntity.class));
        keys.add(this.createKey(ShoppingCartShippingMethodEntity.class));
        keys.add(this.createKey(ShoppingCartTotalsEntity.class));
        keys.add(this.createKey(StoreEntity.class));

        return keys;
    }

    @MetaDataRetriever
    public MetaData getMetadata(MetaDataKey key) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(String.format("%s.%s", ENTITY_PACKAGE, key.getId()));
        return new DefaultMetaData(new DefaultPojoMetaDataModel(clazz));
    }

    /**
     * Adds a comment to the shipment.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:addOrderShipmentComment}
     *
     * @param shipmentId            the shipment's increment id
     * @param comment               the comment to add
     * @param sendEmail             if an email must be sent after shipment creation
     * @param includeCommentInEmail if the comment must be sent in the email
     * @return true if it was successful
     */
    @Processor
    public boolean addOrderShipmentComment(String shipmentId,
                                           String comment,
                                           @Default("false") boolean sendEmail,
                                           @Default("false") boolean includeCommentInEmail)

    {
        return orderClient.addOrderShipmentComment(shipmentId, comment, sendEmail, includeCommentInEmail);
    }

    /**
     * Adds a new tracking number
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:addOrderShipmentTrack}
     *
     * @param shipmentId  the shipment id
     * @param carrierCode the new track's carrier code
     * @param title       the new track's title
     * @param trackId     the new track's id
     * @return the new track's id
     */
    @Processor
    public int addOrderShipmentTrack(String shipmentId,
                                     String carrierCode,
                                     String title,
                                     String trackId) {
        return orderClient.addOrderShipmentTrack(shipmentId, carrierCode, title, trackId);
    }

    /**
     * Cancels an order
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:cancelOrder}
     *
     * @param orderId the order to cancel
     * @return true if the operation was successful
     */
    @Processor
    public boolean cancelOrder(String orderId) {
        return orderClient.cancelOrder(orderId);
    }

    /**
     * Creates a shipment for order
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:createOrderShipment}
     *
     * @param orderId               the order increment id
     * @param itemsQuantities       orderItemId quantity
     * @param comment               an optional comment
     * @param sendEmail             if an email must be sent after shipment creation
     * @param includeCommentInEmail if the comment must be sent in the email
     * @return the new shipment's id
     */
    @Processor
    public String createOrderShipment(String orderId,
                                      @Default("#[payload]") @Placement(group = "Item Ids and Quantities") List<OrderItemIdQty> itemsQuantities,
                                      @Optional String comment,
                                      @Default("false") boolean sendEmail,
                                      @Default("false") boolean includeCommentInEmail) {
        return orderClient.createOrderShipment(orderId, itemsQuantities, comment, sendEmail, includeCommentInEmail);
    }

    /**
     * Answers the order properties for the given orderId
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getOrder}
     *
     * @param orderId the order whose properties to fetch
     * @return a string-object map of order properties
     */
    @Processor
    public SalesOrderEntity getOrder(String orderId) {
        return orderClient.getOrder(orderId);
    }

    /**
     * Retrieves order invoice information
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getOrderInvoice}
     *
     *
     * @param invoiceId the target invoiceId
     * @return the invoice attributes
     */
    @Processor
    public SalesOrderInvoiceEntity getOrderInvoice(String invoiceId) {
        return orderClient.getOrderInvoice(invoiceId);
    }

    /**
     * Retrieves the order shipment carriers.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getOrderShipmentCarriers}
     *
     * @param orderId the target order id
     * @return a list of {@link Carrier} object
     */
    @Processor
    public List<Carrier> getOrderShipmentCarriers(String orderId) {
        return orderClient.getOrderShipmentCarriers(orderId);
    }

    /**
     * Retrieves the order shipment attributes
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getOrderShipment}
     *
     *
     * @param shipmentId the id of the shipment
     * @return the shipment attributes
     */
    @Processor
    public SalesOrderShipmentEntity getOrderShipment(String shipmentId) {
        return orderClient.getOrderShipment(shipmentId);
    }

    /**
     * Puts order on hold. This operation can be reverted with unholdOrder.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:holdOrder}
     *
     * @param orderId the order to put on hold state
     * @return true if the operation was successful
     */
    @Processor
    public boolean holdOrder(String orderId) {
        return orderClient.holdOrder(orderId);
    }

    /**
     * Lists order attributes that match the given filtering expression.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listOrders}
     *
     *
     *
     * @param filter optional filtering expression - one or more comma-separated
     *               unary or binary predicates, one for each filter, in the form
     *               filterType(attributeName, value), for binary filters or
     *               filterType(attributeName), for unary filters, where filterType is
     *               istrue, isfalse or any of the Magento standard filters. Non-numeric
     *               values need to be escaped using simple quotes.
     * @return a list of string-object maps
     */
    @Processor
    public List<SalesOrderListEntity> listOrders(@org.mule.api.annotations.Query @Optional String filter) {
        return orderClient.listOrders(filter);
    }

    @QueryTranslator
    public String toNativeQuery(Query query) {
        MagentoQueryVisitor visitor = new MagentoQueryVisitor();
        query.getFilterExpression().accept(visitor);

        return visitor.toQuery();
    }

    /**
     * Lists order invoices that match the given filtering expression
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listOrdersInvoices}
     *
     *
     * @param filter optional filtering expression - one or more comma-separated
     *               unary or binary predicates, one for each filter, in the form
     *               filterType(attributeName, value), for binary filters or
     *               filterType(attributeName), for unary filters, where filterType is
     *               istrue, isfalse or any of the Magento standard filters. Non-numeric
     *               values need to be escaped using simple quotes.
     * @return list of string-object maps order attributes
     */
    @Processor
    public List<SalesOrderInvoiceEntity> listOrdersInvoices(@org.mule.api.annotations.Query @Optional String filter) {
        return orderClient.listOrdersInvoices(filter);
    }

    /**
     * Lists order shipment atrributes that match the given
     * optional filtering expression
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listOrdersShipments}
     *
     *
     * @param filter optional filtering expression - one or more comma-separated
     *               unary or binary predicates, one for each filter, in the form
     *               filterType(attributeName, value), for binary filters or
     *               filterType(attributeName), for unary filters, where filterType is
     *               istrue, isfalse or any of the Magento standard filters. Non-numeric
     *               values need to be escaped using simple quotes.
     * @return list of string-object map order shipments attributes
     */
    @Processor
    public List<SalesOrderShipmentEntity> listOrdersShipments(@org.mule.api.annotations.Query @Optional String filter) {
        return orderClient.listOrdersShipments(filter);
    }

    /**
     * Deletes the given track of the given order's shipment
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:deleteOrderShipmentTrack}
     *
     * @param shipmentId the target shipment id
     * @param trackId    the id of the track to delete
     * @return if the tracking number is removed from the shipment
     */
    @Processor
    public boolean deleteOrderShipmentTrack(String shipmentId, String trackId) {
        return orderClient.deleteOrderShipmentTrack(shipmentId, trackId);
    }

    /**
     * Adds a comment to the given order id
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:addOrderComment}
     *
     * @param orderId   the order id
     * @param status    the comment status
     * @param comment   the comment
     * @param sendEmail if an email must be sent after shipment creation
     * @return true if the operation was successful
     */
    @Processor
    public boolean addOrderComment(String orderId,
                                   String status,
                                   String comment,
                                   @Default("false") boolean sendEmail) {
        return orderClient.addOrderComment(orderId, status, comment, sendEmail);
    }

    /**
     * Releases order
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:unholdOrder}
     *
     * @param orderId the id of the order to remove from hold state
     * @return true if the operation was successful
     *
     */
    @Processor
    public boolean unholdOrder(String orderId) {
        return orderClient.unholdOrder(orderId);
    }

    /**
     * Creates an invoice for the given order
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:createOrderInvoice}
     *
     * @param orderId               the id of the order
     * @param itemsQuantities       orderItemId quantity
     * @param comment               an optional comment
     * @param sendEmail             if an email must be sent after shipment creation
     * @param includeCommentInEmail if the comment must be sent in the email
     * @return the new invoice's id
     */
    @Processor
    public String createOrderInvoice(String orderId,
                                     @Default("#[payload]") @Placement(group = "Item Ids and Quantities") List<OrderItemIdQty> itemsQuantities,
                                     @Optional String comment,
                                     @Default("false") boolean sendEmail,
                                     @Default("false") boolean includeCommentInEmail) {
        return orderClient.createOrderInvoice(orderId, itemsQuantities, comment, sendEmail,
                includeCommentInEmail);
    }

    /**
     * Adds a comment to the given order's invoice
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:addOrderInvoiceComment}
     *
     * @param invoiceId             the invoice id
     * @param comment               the comment to add
     * @param sendEmail             if an email must be sent after shipment creation
     * @param includeCommentInEmail if the comment must be sent in the email
     * @return true if it was successful
     */
    @Processor
    public boolean addOrderInvoiceComment(String invoiceId,
                                          String comment,
                                          @Default("false") boolean sendEmail,
                                          @Default("false") boolean includeCommentInEmail) {
        return orderClient.addOrderInvoiceComment(invoiceId, comment, sendEmail, includeCommentInEmail);
    }

    /**
     * Captures and invoice
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:captureOrderInvoice}
     *
     * @param invoiceId the invoice to capture
     */
    @Processor
    public void captureOrderInvoice(String invoiceId) {
        orderClient.captureOrderInvoice(invoiceId);
    }

    /**
     * Voids an invoice
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:voidOrderInvoice}
     *
     * @param invoiceId the invoice id
     * @return true if it was successful
     */
    @Processor
    public boolean voidOrderInvoice(String invoiceId) {
        return orderClient.voidOrderInvoice(invoiceId);
    }

    /**
     * Cancels an order's invoice
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:cancelOrderInvoice}
     *
     * @param invoiceId the invoice id
     */
    @Processor
    public void cancelOrderInvoice(String invoiceId) {
        orderClient.cancelOrderInvoice(invoiceId);
    }

    /**
     * Creates a new address for the given customer using the given address
     * attributes
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:createCustomerAddress}
     *
     * @param customerId the customer
     * @param customerAddress the address attributes
     * @return a new customer address id
     */
    @Processor
    public int createCustomerAddress(int customerId, @Default("#[payload]")
    @Placement(group = "Address Attributes") CustomerAddressEntityCreate customerAddress) {
        return customerClient.createCustomerAddress(customerId, customerAddress);
    }

    /**
     * Creates a customer with the given attributes
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:createCustomer}
     *
     * @param customer the attributes of the new customer
     * @return the new customer id
     */
    @Processor
    public int createCustomer(@Default("#[payload]") @Placement(group = "Customer Attributes") CustomerCustomerEntityToCreate customer) {
        return customerClient.createCustomer(customer);
    }

    /**
     * Deletes a customer given its id
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:deleteCustomer}
     *
     * @param customerId the customer to delete
     * @return if the customer is deleted
     */
    @Processor
    public boolean deleteCustomer(int customerId) {
        return customerClient.deleteCustomer(customerId);
    }

    /**
     * Deletes a Customer Address
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:deleteCustomerAddress}
     *
     * @param addressId the address id to delete
     * @return if the customer address is deleted
     */
    @Processor
    public boolean deleteCustomerAddress(int addressId) {
        return customerClient.deleteCustomerAddress(addressId);
    }

    /**
     * Answers customer attributes for the given id. Only the selected attributes are
     * retrieved
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getCustomer}
     *
     * @param customerId     the id of the customer to retrieve
     * @param attributeNames the attributes to retrieve. Not empty
     * @return the attributes map
     */
    @Processor
    public CustomerCustomerEntity getCustomer(int customerId, @Placement(group = "Attributes to Retrieve") List<String> attributeNames) {
        return customerClient.getCustomer(customerId, attributeNames);
    }

    /**
     * Answers the customer address attributes
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getCustomerAddress}
     *
     *
     * @param addressId the id of the address
     * @return the customer address attributes map
     */
    @Processor
    public CustomerAddressEntityItem getCustomerAddress(int addressId) {
        return customerClient.getCustomerAddress(addressId);
    }

    /**
     * Lists the customer address for a given customer id
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listCustomerAddresses}
     *
     *
     * @param customerId the id of the customer
     * @return a listing of addresses
     */
    @Processor
    public List<CustomerAddressEntityItem> listCustomerAddresses(int customerId) {
        return customerClient.listCustomerAddresses(customerId);
    }

    /**
     * Lists all the customer groups
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listCustomerGroups}
     *
     * @return a listing of groups attributes
     */
    @Processor
    public List<CustomerGroupEntity> listCustomerGroups() {
        return customerClient.listCustomerGroups();
    }

    /**
     * Answers a list of customer attributes for the given filter expression.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listCustomers}
     *
     *
     * @param filter optional filtering expression - one or more comma-separated
     *               unary or binary predicates, one for each filter, in the form
     *               filterType(attributeName, value), for binary filters or
     *               filterType(attributeName), for unary filters, where filterType is
     *               istrue, isfalse or any of the Magento standard filters. Non-numeric
     *               values need to be escaped using simple quotes.
     * @return the list of attributes map
     */
    @Processor
    public List<CustomerCustomerEntity> listCustomers(@org.mule.api.annotations.Query @Optional String filter) {
        return customerClient.listCustomers(filter);
    }

    /**
     * Updates the given customer attributes, for the given customer id. Password can
     * not be updated using this method
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateCustomer}
     *
     * @param customerId the target customer to update
     * @param customer the customer attributes
     * @return update result
     */
    @Processor
    public boolean updateCustomer(int customerId,
                                  @Default("#[payload]") @Placement(group = "Customer Attributes to Update") CustomerCustomerEntityToCreate customer) {
        return customerClient.updateCustomer(customerId, customer);
    }

    /**
     * Updates the given map of customer address attributes, for the given customer address
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateCustomerAddress}
     *
     * @param addressId  the customer address to update
     * @param customerAddress the address attributes to update
     * @return update result
     */
    @Processor
    public boolean updateCustomerAddress(int addressId, @Default("#[payload]") @Placement(group = "Address Attributes to Update") CustomerAddressEntityCreate customerAddress) {
        return customerClient.updateCustomerAddress(addressId, customerAddress);
    }

    /**
     * Retrieve stock data by product ids
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listStockItems}
     *
     *
     * @param productIdOrSkus a not empty list of product ids or skus whose attributes to list
     * @return a list of stock items attributes
     */
    @Processor
    public List<CatalogInventoryStockItemEntity> listStockItems(@Placement(group = "Product Ids or SKUs") List<String> productIdOrSkus) {
        return inventoryClient.listStockItems(productIdOrSkus);
    }


    /**
     * Update product stock data given its id or sku
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateStockItem}
     *
     * @param productIdOrSku the product id or sku of the product to update
     * @param stockItem     the attributes to update
     * @return update result
     */
    @Processor
    public int updateStockItem(String productIdOrSku,
                               @Default("#[payload]") @Placement(group = "Attributes to Update") CatalogInventoryStockItemUpdateEntity stockItem) {
        return inventoryClient.updateStockItem(productIdOrSku, stockItem);
    }

    /**
     * Answers the list of countries
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listDirectoryCountries}
     *
     * @return a collection of countries attributes
     */
    @Processor
    public List<DirectoryCountryEntity> listDirectoryCountries() {
        return directoryClient.listDirectoryCountries();
    }

    /**
     * Answers a list of regions for the given county
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listDirectoryRegions}
     *
     *
     * @param countryId the country code, in ISO2 or ISO3 format
     * @return the collection of regions attributes
     */
    @Processor
    public List<DirectoryRegionEntity> listDirectoryRegions(String countryId) {
        return directoryClient.listDirectoryRegions(countryId);
    }


    /**
     * Links two products, given its source and destination productIdOrSku.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:addProductLink}
     *
     * @param type                 the product type
     * @param productId            the id of the source product. Use it instead of productIdOrSku
     *                             in case you are sure the source product identifier is a
     *                             product id
     * @param productSku           the sku of the source product. Use it instead of productIdOrSku
     *                             in case you are sure the source product identifier is a
     *                             product sku
     * @param productIdOrSku       the id or sku of the source product.
     * @param linkedProductIdOrSku the destination product id or sku.
     * @param productLinkEntity           the link attributes
     * @return true if the operation was successful
     */
    @Processor
    public boolean addProductLink(String type,
                                  @Optional Integer productId,
                                  @Optional String productSku,
                                  @Optional String productIdOrSku,
                                  String linkedProductIdOrSku,
                                  @Default("#[payload]") @Placement(group = "Address Attributes to Update") CatalogProductLinkEntity productLinkEntity) {
        return catalogClient.addProductLink(type, ProductIdentifiers.from(productSku, productId, productIdOrSku), linkedProductIdOrSku,
                productLinkEntity);
    }
    
    /**
     * Links two products, given its source and destination productIdOrSku.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:addProductLink}
     *
     * @param attribute            the product type
     * @return true if the operation was successful
     */
    @Processor
    public CatalogProductAttributeEntity getCatalogProductAttributeInfo(String attribute) {
        return catalogClient.getCatalogProductAttributeInfo(attribute);
    }
    
    /**
     * Creates a new product media. See catalog-product-attribute-media-create SOAP
     * method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:createProductAttributeMedia}
     *
     * @param productId         the id of the product. Use it instead of productIdOrSku
     *                          in case you are sure the product identifier is a
     *                          product id
     * @param productSku        the sku of the product. Use it instead of productIdOrSku
     *                          in case you are sure the product identifier is a
     *                          product sku
     * @param productIdOrSku    the id or sku of the product.
     * @param catalogProductAttributeMediaEntity        the media attributes
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @param payload           the image to upload. It may be a file, an input stream or a byte array
     * @param mimeType          the mimetype
     * @param baseFileName      the base name of the new remote image
     * @return the new image filename
     * @throws FileNotFoundException when the payload object is not a valid file
     */
    @Processor
    public String createProductAttributeMedia(@Optional Integer productId,
                                              @Optional String productSku,
                                              @Optional String productIdOrSku,
                                              @Default("#[payload]") @Placement(group = "Media Attributes") CatalogProductAttributeMediaCreateEntity catalogProductAttributeMediaEntity,
                                              @Optional String storeViewIdOrCode,
                                              @Payload Object payload,
                                              MediaMimeType mimeType,
                                              @Optional String baseFileName) throws FileNotFoundException {
        return catalogClient.createProductAttributeMedia(ProductIdentifiers.from(productSku, productId, productIdOrSku),
                catalogProductAttributeMediaEntity, createContent(payload), mimeType, baseFileName, storeViewIdOrCode);
    }

    private InputStream createContent(Object content) throws FileNotFoundException {
        if (content instanceof InputStream) {
            return (InputStream) content;
        }
        if (content instanceof File) {
            return new FileInputStream((File) content);
        }
        if (content instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) content);
        }
        throw new IllegalArgumentException("Unsupported Content Type " + content);
    }

    /**
     * Removes a product image. See catalog-product-attribute-media-remove SOAP
     * method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:deleteProductAttributeMedia}
     *
     * @param productId      the id of the product. Use it instead of productIdOrSku
     *                       in case you are sure the product identifier is a
     *                       product id
     * @param productSku     the sku of the product. Use it instead of productIdOrSku
     *                       in case you are sure the product identifier is a
     *                       product sku
     * @param productIdOrSku the id or sku of the product.
     * @param fileName       the remote media file to delete
     * @return if the image is removed from a product
     */
    @Processor
    public boolean deleteProductAttributeMedia(@Optional Integer productId,
                                               @Optional String productSku,
                                               @Optional String productIdOrSku,
                                               String fileName)

    {
        return catalogClient.deleteProductAttributeMedia(ProductIdentifiers.from(productSku, productId, productIdOrSku), fileName);
    }

    /**
     * Deletes a product's link.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:deleteProductLink}
     *
     * @param type                 link type
     * @param productId            the id of the source product. Use it instead of productIdOrSku
     *                             in case you are sure the source product identifier is a
     *                             product id
     * @param productSku           the sku of the source product. Use it instead of productIdOrSku
     *                             in case you are sure the source product identifier is a
     *                             product sku
     * @param productIdOrSku       the id or sku of the source product.
     * @param linkedProductIdOrSku the linked product id or sku
     * @return if the link is removed from a product
     */
    @Processor
    public boolean deleteProductLink(String type,
                                     @Optional Integer productId,
                                     @Optional String productSku,
                                     @Optional String productIdOrSku,
                                     String linkedProductIdOrSku) {
        return catalogClient.deleteProductLink(type, ProductIdentifiers.from(productSku, productId, productIdOrSku), linkedProductIdOrSku);
    }

    /**
     * Lists linked products to the given product and for the given link type.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getProductAttributeMedia}
     *
     *
     * @param productId         the id of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product id
     * @param productSku        the sku of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product sku
     * @param productIdOrSku    the id or sku of the product.
     * @param fileName          the name of the file
     * @param storeViewIdOrCode the store view id or code
     * @return the list of links to the product
     */
    @Processor
    public CatalogProductImageEntity getProductAttributeMedia(@Optional Integer productId,
                                                              @Optional String productSku,
                                                              @Optional String productIdOrSku,
                                                              String fileName,
                                                              @Optional String storeViewIdOrCode) {
        return catalogClient.getProductAttributeMedia(ProductIdentifiers.from(productSku, productId, productIdOrSku), fileName, storeViewIdOrCode);
    }


    /**
     * Answers the current default catalog store view id for this session
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getCatalogCurrentStoreView}
     *
     * @return the current default store view id
     */
    @Processor
    public int getCatalogCurrentStoreView() {
        return catalogClient.getCatalogCurrentStoreView();
    }

    /**
     * Set the default catalog store view for this session
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateCategoryAttributeStoreView}
     *
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     *                          the id or code of the store view to set as default for this
     *                          session
     * @return update result
     */
    @Processor
    public int updateCategoryAttributeStoreView(String storeViewIdOrCode) {
        return catalogClient.updateCatalogCurrentStoreView(storeViewIdOrCode);
    }
    
    /**
     * Retrieve product image types. See catalog-product-attribute-media-types SOAP
     * method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listCategoryAttributes}
     *
     * @return the list of category attributes
     */
    @Processor
    public List<CatalogAttributeEntity> listCategoryAttributes() {
        return catalogClient.listCategoryAttributes();
    }

    /**
     * Retrieves attribute options. See catalog-category-attribute-options SOAP
     * method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listCategoryAttributeOptions}
     *
     *
     * @param attributeId       the target attribute whose options will be retrieved
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return the list of category attribute options
     */
    @Processor
    public List<CatalogAttributeOptionEntity> listCategoryAttributeOptions(String attributeId,
                                                                           @Optional String storeViewIdOrCode) {
        return catalogClient.listCategoryAttributeOptions(attributeId, storeViewIdOrCode);
    }

    /**
     * Retrieves product image list. See catalog-product-attribute-media-list SOAP
     * method
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductAttributeMedia}
     *
     *
     * @param productId         the id of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product id
     * @param productSku        the sku of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product sku
     * @param productIdOrSku    the id or sku of the product.
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return the list of product images attributes
     */
    @Processor
    public List<CatalogProductImageEntity> listProductAttributeMedia(@Optional Integer productId,
                                                                     @Optional String productSku,
                                                                     @Optional String productIdOrSku,
                                                                     @Optional String storeViewIdOrCode) {
        return catalogClient.listProductAttributeMedia(ProductIdentifiers.from(productSku, productId, productIdOrSku), storeViewIdOrCode);
    }

    /**
     * Retrieve product image types. See catalog-product-attribute-media-types SOAP
     * method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductAttributeMediaTypes}
     *
     *
     * @param setId the setId
     * @return the list of attribute media types
     */
    @Processor
    public List<CatalogProductAttributeMediaTypeEntity> listProductAttributeMediaTypes(int setId) {
        return catalogClient.listProductAttributeMediaTypes(setId);
    }

    /**
     * Answers the product attribute options. See catalog-product-attribute-options
     * SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductAttributeOptions}
     *
     *
     * @param attributeId       the target attribute whose options will be listed
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return the attributes list
     */
    @Processor
    public List<CatalogAttributeOptionEntity> listProductAttributeOptions(String attributeId,
                                                                          @Optional String storeViewIdOrCode) {
        return catalogClient.listProductAttributeOptions(attributeId, storeViewIdOrCode);
    }


    /**
     * Retrieves product attributes list. See catalog-product-attribute-list SOAP
     * methods
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductAttributes}
     *
     *
     * @param setId the set id
     * @return the list of product attributes
     */
    @Processor
    public List<CatalogAttributeEntity> listProductAttributes(int setId) {
        return catalogClient.listProductAttributes(setId);
    }

    /**
     * Retrieves product attribute sets. See catalog-product-attribute-set-list SOAP
     * method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductAttributeSets}
     *
     * @return The list of product attributes sets
     */
    @Processor
    public List<CatalogProductAttributeSetEntity> listProductAttributeSets() {
        return catalogClient.listProductAttributeSets();
    }

    /**
     * Retrieve product tier prices. See catalog-product-attribute-tier-price-info
     * SOAP Method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductAttributeTierPrices}
     *
     *
     * @param productId      the id of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product id
     * @param productSku     the sku of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product sku
     * @param productIdOrSku the id or sku of the product.
     * @return the list of product attributes
     */
    @Processor
    public List<CatalogProductTierPriceEntity> listProductAttributeTierPrices(@Optional Integer productId,
                                                                              @Optional String productSku,
                                                                              @Optional String productIdOrSku) {
        return catalogClient.listProductAttributeTierPrices(ProductIdentifiers.from(productSku, productId, productIdOrSku));
    }

    /**
     * Lists linked products to the given product and for the given link type
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductLink}
     *
     *
     * @param type           the link type
     * @param productId      the id of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product id
     * @param productSku     the sku of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product sku
     * @param productIdOrSku the id or sku of the product.
     * @return the list of links to the product
     */
    @Processor
    public List<CatalogProductLinkEntity> listProductLink(String type,
                                                          @Optional Integer productId,
                                                          @Optional String productSku,
                                                          @Optional String productIdOrSku) {
        return catalogClient.listProductLink(type, ProductIdentifiers.from(productSku, productId, productIdOrSku));
    }


    /**
     * Lists all the attributes for the given product link type
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductLinkAttributes}
     *
     *
     * @param type the product type
     * @return the listing of product attributes
     */
    @Processor
    public List<CatalogProductLinkAttributeEntity> listProductLinkAttributes(String type) {
        return catalogClient.listProductLinkAttributes(type);
    }

    /**
     * Answers product link types
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductLinkTypes}
     *
     * @return the list of product link types
     */
    @Processor
    public List<String> listProductLinkTypes() {
        return catalogClient.listProductLinkTypes();
    }

    /**
     * Answers product types. See catalog-product-type-list SOAP method
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProductTypes}
     *
     * @return the list of product types
     */
    @Processor
    public List<CatalogProductTypeEntity> listProductTypes() {
        return catalogClient.listProductTypes();
    }

    /**
     * Updates product media. See catalog-product-attribute-media-update
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateProductAttributeMedia}
     *
     * @param productId         the id of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product id
     * @param productSku        the sku of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product sku
     * @param productIdOrSku    the id or sku of the product.
     * @param fileName          the name of the remote media file to update
     * @param catalogProductAttributeMediaEntity        the attributes to update
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return update result
     */
    @Processor
    public boolean updateProductAttributeMedia(@Optional Integer productId,
                                               @Optional String productSku,
                                               @Optional String productIdOrSku,
                                               String fileName,
                                               @Default("#[payload]") @Placement(group = "Media Attributes to Update") CatalogProductAttributeMediaCreateEntity catalogProductAttributeMediaEntity,
                                               @Optional String storeViewIdOrCode) {
        return catalogClient.updateProductAttributeMedia(ProductIdentifiers.from(productSku, productId, productIdOrSku), fileName,
                catalogProductAttributeMediaEntity, storeViewIdOrCode);
    }


    /**
     * Updates a single product tier price. See catalog-product-attribute-tier-price-update
     * SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateProductAttributeTierPrice}
     *
     * @param productId      the id of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product id
     * @param productSku     the sku of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product sku
     * @param productIdOrSku the id or sku of the product.
     * @param catalogProductTierPriceEntity     the tier price to update.
     * @return update result
     */
    @Processor
    public int updateProductAttributeTierPrice(@Optional Integer productId,
                                               @Optional String productSku,
                                               @Optional String productIdOrSku,
                                               @Default("#[payload]") @Placement(group = "Tier Price Attributes to Update") CatalogProductTierPriceEntity catalogProductTierPriceEntity) {
        return catalogClient.updateProductAttributeTierPrice(ProductIdentifiers.from(productSku, productId, productIdOrSku), catalogProductTierPriceEntity);
    }


    /**
     * Update product link
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateProductLink}
     *
     * @param type                 the link type
     * @param productId            the id of the source product. Use it instead of productIdOrSku
     *                             in case you are sure the source product identifier is a
     *                             product id
     * @param productSku           the sku of the source product. Use it instead of productIdOrSku
     *                             in case you are sure the source product identifier is a
     *                             product sku
     * @param productIdOrSku       the id or sku of the source product.
     * @param linkedProductIdOrSku the destination product id or sku.
     * @param catalogProductLinkEntity           the link attributes
     * @return update result
     */
    @Processor
    public boolean updateProductLink(String type,
                                     @Optional Integer productId,
                                     @Optional String productSku,
                                     @Optional String productIdOrSku,
                                     String linkedProductIdOrSku,
                                     @Default("#[payload]") @Placement(group = "Link Attributes to Update") CatalogProductLinkEntity catalogProductLinkEntity) {
        return catalogClient.updateProductLink(type, ProductIdentifiers.from(productSku, productId, productIdOrSku), linkedProductIdOrSku,
                catalogProductLinkEntity);
    }

    /**
     * Lists product of a given category. See  catalog-category-assignedProducts SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listCategoryProducts}
     *
     *
     * @param categoryId the category
     * @return the listing of category products
     */
    @Processor
    public List<CatalogAssignedProduct> listCategoryProducts(int categoryId) {
        return catalogClient.listCategoryProducts(categoryId);
    }

    /**
     * Assign product to category. See catalog-category-assignProduct SOAP method
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:addCategoryProduct}
     *
     * @param categoryId     the category where the given product will be added
     * @param productId      the id of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product id
     * @param productSku     the sku of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product sku
     * @param productIdOrSku the id or sku of the product.
     * @param position       the position of this product
     * @return true if the operation was successful
     */
    @Processor
    public boolean addCategoryProduct(int categoryId,
                                      @Optional Integer productId,
                                      @Optional String productSku,
                                      @Optional String productIdOrSku,
                                      @Optional String position) {
        return catalogClient.addCategoryProduct(
                categoryId, ProductIdentifiers.from(productSku, productId, productIdOrSku), position);
    }

    /**
     * Creates a new category. See catalog-category-create SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:createCategory}
     *
     * @param parentId          the parent category id
     * @param catalogCategoryEntity        the new category attributes
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return the new category id
     */
    @Processor
    public int createCategory(int parentId,
                              @Default("#[payload]") @Placement(group = "Category Attributes") CatalogCategoryEntityCreate catalogCategoryEntity,
                              @Optional String storeViewIdOrCode) {
        return catalogClient.createCategory(parentId, catalogCategoryEntity, storeViewIdOrCode);
    }

    /**
     * Deletes a category. See  catalog-category-delete SOAP method
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:deleteCategory}
     *
     * @param categoryId the category to delete
     * @return if the category is deleted
     */
    @Processor
    public boolean deleteCategory(int categoryId) {
        return catalogClient.deleteCategory(categoryId);
    }

    /**
     * Answers category attributes. See catalog-category-info SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getCategory}
     *
     *
     * @param categoryId        the category whose attributes will be retrieved
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified
     *                          for using current store
     * @param attributeNames    the category attributes that will be retrieved
     * @return the category attributes
     */
    @Processor
    public CatalogCategoryInfo getCategory(int categoryId,
                                           @Optional String storeViewIdOrCode,
                                           @Placement(group = "Attribute Names to Retrieve") List<String> attributeNames) {
        return catalogClient.getCategory(categoryId, storeViewIdOrCode, attributeNames);
    }

    /**
     * Answers levels of categories for a website, store view and parent category
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listCategoryLevels}
     *
     *
     * @param website           the website
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @param parentCategoryId  the parent category of the categories that will be listed
     * @return the list of categories attributes
     */
    @Processor
    public List<CatalogCategoryEntityNoChildren> listCategoryLevels(@Optional String website,
                                                                    @Optional String storeViewIdOrCode,
                                                                    @Optional String parentCategoryId)

    {
        return catalogClient.listCategoryLevels(website, storeViewIdOrCode, parentCategoryId);
    }

    /**
     * Move category in tree. See  catalog-category-move SOAP method. Please make sure that you are not
     * moving category to any of its own children. There are no
     * extra checks to prevent doing it through webservices API, and you won’t be
     * able to fix this from admin interface then .
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:moveCategory}
     *
     * @param categoryId the id of the category to be moved
     * @param parentId   the new parent category id
     * @param afterId    an optional category id for use as reference in the positioning of the moved category
     */
    @Processor
    public void moveCategory(int categoryId,
                             int parentId,
                             @Optional String afterId) {
        catalogClient.moveCategory(categoryId, parentId, afterId);
    }

    /**
     * Remove a product assignment. See catalog-category-removeProduct SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:deleteCategoryProduct}
     *
     * @param categoryId     the category to delete
     * @param productId      the id of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product id
     * @param productSku     the sku of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product sku
     * @param productIdOrSku the id or sku of the product.
     * @return if the product is removed from the category
     */
    @Processor
    public boolean deleteCategoryProduct(int categoryId,
                                         @Optional Integer productId,
                                         @Optional String productSku,
                                         @Optional String productIdOrSku) {
        return catalogClient.deleteCategoryProduct(categoryId, ProductIdentifiers.from(productSku, productId, productIdOrSku));
    }

    /**
     * Answers the category tree. See catalog-category-tree SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getCategoryTree}
     *
     *
     * @param parentId          the parent id
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return a category tree attributes
     */
    @Processor
    public CatalogCategoryTree getCategoryTree(String parentId,
                                               @Optional String storeViewIdOrCode)

    {
        return catalogClient.getCategoryTree(parentId, storeViewIdOrCode);
    }

    /**
     * Updates a category. See catalog-category-update SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateCategory}
     *
     * @param categoryId        the category to update
     * @param catalogCategoryEntity        the category new attributes
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return update results
     */
    @Processor
    public boolean updateCategory(int categoryId,
                                  @Default("#[payload]") @Placement(group = "Category Attributes to Update") CatalogCategoryEntityCreate catalogCategoryEntity,
                                  @Optional String storeViewIdOrCode) {
        return catalogClient.updateCategory(categoryId, catalogCategoryEntity, storeViewIdOrCode);
    }

    /**
     * Updates a category product.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateCategoryProduct}
     *
     * @param categoryId     the category id
     * @param productId      the id of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product id
     * @param productSku     the sku of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product sku
     * @param productIdOrSku the id or sku of the product.
     * @param position       the category position for ordering the category inside its level
     * @return update result
     */
    @Processor
    public boolean updateCategoryProduct(int categoryId,
                                         @Optional Integer productId,
                                         @Optional String productSku,
                                         @Optional String productIdOrSku,
                                         String position) {
        return catalogClient.updateCategoryProduct(categoryId, ProductIdentifiers.from(productSku, productId, productIdOrSku), position);
    }

    /**
     * Lists inventory stock items.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listInventoryStockItems}
     *
     *
     * @param productIdOrSkus the list of product ids and/or skus whose attributes will be retrieved
     * @return the list of attributes
     */
    @Processor
    public List<CatalogInventoryStockItemEntity> listInventoryStockItems(@Placement(group = "Product Ids or SKUs") List<String> productIdOrSkus) {
        return catalogClient.listInventoryStockItems(productIdOrSkus);
    }

    /**
     * Updates an stock inventory item
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateInventoryStockItem}
     *
     * @param productId      the id of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product id
     * @param productSku     the sku of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product sku
     * @param productIdOrSku the id or sku of the product.
     * @param catalogInventoryStockItem     the new attributes of the stock item
     * @return update result
     */
    @Processor
    public int updateInventoryStockItem(@Optional Integer productId,
                                        @Optional String productSku,
                                        @Optional String productIdOrSku,
                                        @Default("#[payload]") @Placement(group = "Stock Item Attributes") CatalogInventoryStockItemUpdateEntity catalogInventoryStockItem) {
        return catalogClient.updateInventoryStockItem(ProductIdentifiers.from(productSku, productId, productIdOrSku), catalogInventoryStockItem);
    }

    /**
     * Creates a new product
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:createProduct}
     *
     * @param type                 the new product's type
     * @param set                  the new product's set
     * @param sku                  the new product's sku
     * @param attributes           the standard attributes of the new product
     * @param additionalAttributes the non standard attributes of the new product
     * @param storeViewIdOrCode    the id or code of the target store. Left unspecified for using current store
     * @return the new product's id
     */
    @Processor
    public int createProduct(String type,
                             int set,
                             String sku,
                             @Default("#[payload]") @Placement(group = "Standard Product Attributes") CatalogProductCreateEntity attributes,
                             @Placement(group = "Non-standard Product Attributes") @Optional List<AssociativeEntity> additionalAttributes,
                             @Optional String storeViewIdOrCode) {
        return catalogClient.createProduct(type, set, sku, attributes, additionalAttributes, storeViewIdOrCode);
    }


    /**
     * Deletes a product. See catalog-product-delete SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:deleteProduct}
     *
     * @param productId      the id of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product id
     * @param productSku     the sku of the product. Use it instead of productIdOrSku in
     *                       case you are sure the product identifier is a product sku
     * @param productIdOrSku the id or sku of the product.
     * @return if the product is deleted
     */
    @Processor
    public int deleteProduct(@Optional Integer productId,
                             @Optional String productSku,
                             @Optional String productIdOrSku) {
        return catalogClient.deleteProduct(ProductIdentifiers.from(productSku, productId, productIdOrSku));
    }


    /**
     * Answers a product special price. See catalog-product-getSpecialPrice SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getProductSpecialPrice}
     *
     *
     * @param productId         the id of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product id
     * @param productSku        the sku of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product sku
     * @param productIdOrSku    the id or sku of the product.
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return the product special price attributes
     */
    @Processor
    public CatalogProductReturnEntity getProductSpecialPrice(@Optional Integer productId,
                                                             @Optional String productSku,
                                                             @Optional String productIdOrSku,
                                                             @Optional String storeViewIdOrCode) {
        return catalogClient.getProductSpecialPrice(ProductIdentifiers.from(productSku, productId, productIdOrSku),
                storeViewIdOrCode);
    }

    /**
     * Answers a product's specified attributes. At least one of attributNames or
     * additionalAttributeNames must be non null and non empty. See
     * catalog-product-info SOAP method
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getProduct}
     *
     *
     * @param productId                the id of the product. Use it instead of productIdOrSku in
     *                                 case you are sure the product identifier is a product id
     * @param productSku               the sku of the product. Use it instead of productIdOrSku in
     *                                 case you are sure the product identifier is a product sku
     * @param productIdOrSku           the id or sku of the product.
     * @param storeViewIdOrCode        the id or code of the target store. Left unspecified for using current store
     * @param attributesNames          the list of standard attributes to be returned
     * @param additionalAttributeNames the list of non standard attributes to be returned in the additionalAttributes attribute
     * @return the attributes
     */
    @Processor
    public CatalogProductReturnEntity getProduct(@Optional Integer productId,
                                                 @Optional String productSku,
                                                 @Optional String productIdOrSku,
                                                 @Optional String storeViewIdOrCode,
                                                 @Placement(group = "Standard Product Attributes to Retrieve") @Optional List<String> attributesNames,
                                                 @Placement(group = "Non-standard Product Attributes to Retrieve") @Optional List<String> additionalAttributeNames) {
        return catalogClient.getProduct(ProductIdentifiers.from(productSku, productId, productIdOrSku), storeViewIdOrCode, attributesNames, additionalAttributeNames);
    }

    /**
     * Retrieve products list by filters. See catalog-product-list SOAP method.
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listProducts}
     *
     *
     * @param filter            optional filtering expression - one or more comma-separated
     *                          unary or binary predicates, one for each filter, in the form
     *                          filterType(attributeName, value), for binary filters or
     *                          filterType(attributeName), for unary filters, where filterType is
     *                          istrue, isfalse or any of the Magento standard filters. Non-numeric
     *                          values need to be escaped using simple quotes.
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return the list of product attributes that match the given optional filtering expression
     */
    @Processor
    public List<CatalogProductEntity> listProducts(@org.mule.api.annotations.Query @Optional String filter,
                                                   @Optional String storeViewIdOrCode) {
        return catalogClient.listProducts(filter, storeViewIdOrCode);
    }


    /**
     * Sets a product special price. See catalog-product-setSpecialPrice SOAP method
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateProductSpecialPrice}
     *
     * @param productId         the id of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product id
     * @param productSku        the sku of the product. Use it instead of productIdOrSku in
     *                          case you are sure the product identifier is a product sku
     * @param productIdOrSku    the id or sku of the product.
     * @param specialPrice      the special price to set
     * @param fromDate          optional start date of the special price period
     * @param toDate            optional end date of the special price period
     * @param storeViewIdOrCode the id or code of the target store. Left unspecified for using current store
     * @return update result
     */
    @Processor
    public int updateProductSpecialPrice(@Optional Integer productId,
                                         @Optional String productSku,
                                         @Optional String productIdOrSku,
                                         String specialPrice,
                                         @Optional String fromDate,
                                         @Optional String toDate,
                                         @Optional String storeViewIdOrCode) {
        return catalogClient.updateProductSpecialPrice(ProductIdentifiers.from(productSku, productId, productIdOrSku), specialPrice,
                fromDate, toDate, storeViewIdOrCode);
    }

    /**
     * Updates a product. At least one of attributes or additionalAttributes 
     * must be non null and non empty. See catalog-category-updateProduct SOAP method
     * <p/>
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateProduct}
     *
     * @param productId            the id of the product. Use it instead of productIdOrSku in
     *                             case you are sure the product identifier is a product id
     * @param productSku           the sku of the product. Use it instead of productIdOrSku in
     *                             case you are sure the product identifier is a product sku
     * @param productIdOrSku       the id or sku of the product.
     * @param catalogProductEntity           standard product attributes to update
     * @param additionalAttributes non standard product attributes to update
     * @param storeViewIdOrCode    the id or code of the target store. Left unspecified for using current store
     * @return update result
     */
    @Processor
    public boolean updateProduct(@Optional Integer productId,
                                 @Optional String productSku,
                                 @Optional String productIdOrSku,
                                 @Default("#[payload]") @Placement(group = "Standard Product Attributes to Update") CatalogProductCreateEntity catalogProductEntity,
                                 @Placement(group = "Non-standard Product Attributes to Update") @Optional List<AssociativeEntity> additionalAttributes,
                                 @Optional String storeViewIdOrCode) {
        return catalogClient.updateProduct(ProductIdentifiers.from(productSku, productId, productIdOrSku), catalogProductEntity, additionalAttributes, storeViewIdOrCode);
    }

    /**
     * Creates an empty shopping cart. Store Id is optional, but some operations may not work correctly if isn't given in the cart creation.
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:createShoppingCart}
     *
     * @param storeId Store view ID or code
     * @return ID of the created empty shopping cart
     */
    @Processor
    public int createShoppingCart(@Optional String storeId) {
        return shoppingCartClient.createShoppingCart(storeId);
    }

    /**
     * Retrieves full information about the shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:getInfoShoppingCart}
     *
     *
     * @param quoteId Shopping cart ID (quote ID)
     * @param storeId Store view ID or code
     * @return shopping cart info
     */
    @Processor
    public ShoppingCartInfoEntity getInfoShoppingCart(int quoteId, @Optional String storeId) {
        return shoppingCartClient.getShoppingCartInfo(quoteId, storeId);
    }

    /**
     * Retrieves total prices for a shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listShoppingCartTotals}
     *
     *
     * @param quoteId Shopping cart ID (quote identifier)
     * @param storeId Store view ID or code
     * @return shopping cart total prices
     */
    @Processor
    public List<ShoppingCartTotalsEntity> listShoppingCartTotals(int quoteId, @Optional String storeId) {
        return shoppingCartClient.listShoppingCartTotals(quoteId, storeId);
    }

    /**
     * Creates an order from a shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:createShoppingCartOrder}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param storeId Store view ID or code
     * @param licenses Website license ID
     * @return result code of order creation
     */
    @Processor
    public String createShoppingCartOrder(int quoteId, @Optional String storeId, @Placement(group = "Licenses") @Optional List<String> licenses) {
        return shoppingCartClient.createShoppingCartOrder(quoteId, storeId, licenses);
    }

    /**
     * Retrieves the website license agreement for the quote according to the website (store). Store id must be given in cart creation.
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listShoppingCartLicenses}
     *
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param storeId Store view ID or code
     * @return licences for the shopping cart
     */
    @Processor
    public List<ShoppingCartLicenseEntity> listShoppingCartLicenses(int quoteId, @Optional String storeId) {
        return shoppingCartClient.listShoppingCartLicenses(quoteId, storeId);
    }

    /**
     * Adds one or more products to the shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:addShoppingCartProduct}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param products Products data
     * @param storeId Store view ID or code
     * @return True on success (if the product is added to the shopping cart)
     */
    @Processor
    public boolean addShoppingCartProduct(int quoteId,
                                          @Default("#[payload]") @Placement(group = "Products attributes") List<ShoppingCartProductEntity> products,
                                          @Optional String storeId) {
        return shoppingCartClient.addShoppingCartProduct(quoteId, products, storeId);
    }

    /**
     * Updates one or several products in the shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:updateShoppingCartProduct}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param products Products data
     * @param storeId Store view ID or code
     * @return True if the product is updated
     */
    @Processor
    public boolean updateShoppingCartProduct(int quoteId,
                                             @Default("#[payload]") @Placement(group = "Products attributes") List<ShoppingCartProductEntity> products,
                                             @Optional String storeId) {
        return shoppingCartClient.updateShoppingCartProduct(quoteId, products, storeId);
    }

    /**
     * Removes one or several products from a shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:removeShoppingCartProduct}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param products Products data
     * @param storeId Store view ID or code
     * @return True if the product is removed
     */
    @Processor
    public boolean removeShoppingCartProduct(int quoteId,
                                             @Default("#[payload]") @Placement(group = "Products attributes") List<ShoppingCartProductEntity> products,
                                             @Optional String storeId) {
        return shoppingCartClient.removeShoppingCartProduct(quoteId, products, storeId);
    }

    /**
     * Retrieves the list of products in the shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listShoppingCartProducts}
     *
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param storeId Store view ID or code
     * @return products in the shopping cart.
     */
    @Processor
    public List<CatalogProductEntity> listShoppingCartProducts(int quoteId, @Optional String storeId) {
        return shoppingCartClient.listShoppingCartProducts(quoteId, storeId);
    }

    /**
     * Moves products from the current quote to a customer quote.
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:moveShoppingCartProductToCustomerQuote}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param products Products data
     * @param storeId Store view ID or code
     * @return True if the product is moved to customer quote
     */
    @Processor
    public boolean moveShoppingCartProductToCustomerQuote(int quoteId,
                                                          @Default("#[payload]") @Placement(group = "Products attributes") List<ShoppingCartProductEntity> products,
                                                          @Optional String storeId) {
        return shoppingCartClient.moveShoppingCartProductToCustomerQuote(quoteId, products, storeId);
    }

    /**
     * Adds information about the customer to a shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:setShoppingCartCustomer}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param customer Cuestomer data
     * @param storeId Store view ID or code
     * @return True if information is added
     */
    @Processor
    public boolean setShoppingCartCustomer(int quoteId,
                                           @Default("#[payload]") @Placement(group = "Customer attributes") ShoppingCartCustomerEntity customer,
                                           @Optional String storeId) {
        return shoppingCartClient.setShoppingCartCustomer(quoteId, customer, storeId);
    }

    /**
     * Sets the customer addresses in the shopping cart (quote). Don't set a coupon to the cart before using this method, the address won't be set.
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:setShoppingCartCustomerAddresses}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param shoppingCartCustomerAddresses Addresses data (mode is required for each address and should be either "shipping" or "billing")
     * @param storeId Store view ID or code
     * @return True if information is added
     */
    @Processor
    public boolean setShoppingCartCustomerAddresses(int quoteId,
                                                    @Default("#[payload]") @Placement(group = "Addresses attributes") List<ShoppingCartCustomerAddressEntity> shoppingCartCustomerAddresses,
                                                    @Optional String storeId) {
        return shoppingCartClient.setShoppingCartCustomerAddresses(quoteId, shoppingCartCustomerAddresses, storeId);
    }

    /**
     * Retrieves a shipping method for a shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:setShoppingCartShippingMethod}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param method Shipping method code
     * @param storeId Store view ID or code
     * @return True if the shipping method is retrieved
     */
    @Processor
    public boolean setShoppingCartShippingMethod(int quoteId, String method, @Optional String storeId) {
        return shoppingCartClient.setShoppingCartShippingMethod(quoteId, method, storeId);
    }

    /**
     * Retrieves the list of available shipping methods for a shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listShoppingCartShippingMethods}
     *
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param storeId Store view ID or code
     * @return Shipping methods available
     */
    @Processor
    public List<ShoppingCartShippingMethodEntity> listShoppingCartShippingMethods(int quoteId, @Optional String storeId) {
        return shoppingCartClient.listShoppingCartShippingMethods(quoteId, storeId);
    }

    /**
     * Sets a payment method for a shopping cart (quote), cart must not be empty.
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:setShoppingCartPaymentMethod}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param shoppingCartPaymentMethod Payment mehthod data
     * @param storeId Store view ID or code
     * @return True on success
     */
    @Processor
    public boolean setShoppingCartPaymentMethod(int quoteId,
                                                @Default("#[payload]") @Placement(group = "Method attributes") ShoppingCartPaymentMethodEntity shoppingCartPaymentMethod,
                                                @Optional String storeId) {
        return shoppingCartClient.setShoppingCartPaymentMethod(quoteId, shoppingCartPaymentMethod, storeId);
    }

    /**
     * Retrieves a list of available payment methods for a shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:listShoppingCartPaymentMethods}
     *
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param storeId Store view ID or code
     * @return Payment methods available
     */
    @Processor
    public ShoppingCartPaymentMethodResponseEntityArray listShoppingCartPaymentMethods(int quoteId, @Optional String storeId) {
        return shoppingCartClient.listShoppingCartPaymentMethods(quoteId, storeId);
    }

    /**
     * Adds a coupon code for a shopping cart (quote). The shopping cart must not be empty. The shopping cart must be created with a storeId for this operation to work.
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:addShoppingCartCoupon}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param couponCode Coupon code
     * @param storeId Store view ID or code
     * @return True if the coupon code is added
     */
    @Processor
    public boolean addShoppingCartCoupon(int quoteId, String couponCode, @Optional String storeId) {
        return shoppingCartClient.addShoppingCartCoupon(quoteId, couponCode, storeId);
    }

    /**
     * Removes a coupon code from a shopping cart (quote).
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:removeShoppingCartCoupon}
     *
     * @param quoteId Shopping Cart ID (quote ID)
     * @param storeId Store view ID or code
     * @return True if the coupon code is removed
     */
    @Processor
    public boolean removeShoppingCartCoupon(int quoteId, @Optional String storeId) {
        return shoppingCartClient.removeShoppingCartCoupon(quoteId, storeId);
    }

    /**
     * Removes an existing order with increment Id and creates new one with new increment Id and new data. 
     * {@sample.xml ../../../doc/magento-connector.xml.sample magento:remakeOrder}
     * 
     * @param incrementId Increment Id of magento order
     * @param customerData New data for new order
     */
    @Processor
    public void reorderOrder(String incrementId, @Default("#[payload]") SalesOrderEntityToReorder customerData) {
    	orderClient.reorderOrder(incrementId, customerData);
    }
    
    @SuppressWarnings("unchecked")
    public void setOrderClient(MagentoOrderClient<?> magentoOrderClient) {
        this.orderClient = MagentoClientAdaptor.adapt(MagentoOrderClient.class, magentoOrderClient);
    }

    @SuppressWarnings("unchecked")
    public void setCustomerClient(MagentoCustomerClient<?> customerClient) {
        this.customerClient = MagentoClientAdaptor.adapt(MagentoCustomerClient.class, customerClient);
    }

    @SuppressWarnings("unchecked")
    public void setInventoryClient(MagentoInventoryClient<?> inventoryClient) {
        this.inventoryClient = MagentoClientAdaptor.adapt(MagentoInventoryClient.class, inventoryClient);
    }

    @SuppressWarnings("unchecked")
    public void setDirectoryClient(MagentoDirectoryClient<?> directoryClient) {
        this.directoryClient = MagentoClientAdaptor.adapt(MagentoDirectoryClient.class, directoryClient);
    }

    @SuppressWarnings("unchecked")
    public void setCatalogClient(MagentoCatalogClient<?> catalogClient) {
        this.catalogClient = MagentoClientAdaptor.adapt(MagentoCatalogClient.class, catalogClient);
    }

    @SuppressWarnings("unchecked")
    public void setShoppingCartClient(MagentoShoppingCartClient<?> catalogClient) {
        this.shoppingCartClient = MagentoClientAdaptor.adapt(MagentoShoppingCartClient.class, catalogClient);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    private class PortProviderInitializer {
        private DefaultAxisPortProvider provider;
        private String username;
        private String password;
        private String address;

        public PortProviderInitializer(String username, String password, String address) {
            this.username = username;
            this.password = password;
            this.address = address;
        }

        public AxisPortProvider getPortProvider() {
            if (provider == null) {
                provider = new DefaultAxisPortProvider(username, password, address);
            }
            return provider;
        }
    }
}
