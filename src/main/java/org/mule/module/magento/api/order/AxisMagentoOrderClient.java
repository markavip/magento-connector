/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.md file.
 */

package org.mule.module.magento.api.order;

import com.magento.api.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.Validate;
import org.mule.module.magento.api.AbstractMagentoClient;
import org.mule.module.magento.api.AxisPortProvider;
import org.mule.module.magento.api.order.model.Carrier;
import org.mule.module.magento.filters.FiltersParser;

import javax.validation.constraints.NotNull;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.apache.commons.lang.BooleanUtils.toInteger;

public class AxisMagentoOrderClient extends AbstractMagentoClient
    implements MagentoOrderClient<RemoteException>
{

    public AxisMagentoOrderClient(AxisPortProvider provider)
    {
        super(provider);
    }

    public List<SalesOrderListEntity> listOrders(String filter) throws RemoteException
    {
        return Arrays.asList(getPort().salesOrderList(getSessionId(), FiltersParser.parse(filter)));
    }

    public com.magento.api.SalesOrderEntity getOrder(String orderId) throws RemoteException
    {
        return getPort().salesOrderInfo(getSessionId(), orderId);
    }

    public boolean holdOrder(String orderId) throws RemoteException
    {
        return getPort().salesOrderHold(getSessionId(), orderId);
    }

    public boolean unholdOrder(@NotNull String orderId) throws RemoteException
    {
        Validate.notNull(orderId);
        return getPort().salesOrderUnhold(getSessionId(), orderId);
    }

    public boolean cancelOrder(@NotNull String orderId) throws RemoteException
    {
        Validate.notNull(orderId);
        return getPort().salesOrderCancel(getSessionId(), orderId);
    }

    public boolean addOrderComment(@NotNull String orderId,
                                   @NotNull String status,
                                   @NotNull String comment,
                                   boolean sendEmail) throws RemoteException
    {
        Validate.notNull(orderId);
        Validate.notNull(status);
        Validate.notNull(comment);
        return getPort().salesOrderAddComment(getSessionId(), orderId, status, comment, toIntegerString(sendEmail));
    }

    @NotNull
    public List<SalesOrderShipmentEntity> listOrdersShipments(String filter) throws RemoteException
    {
        return Arrays.asList(getPort().salesOrderShipmentList(getSessionId(), FiltersParser.parse(filter)));
    }

    public SalesOrderShipmentEntity getOrderShipment(String shipmentId) throws RemoteException
    {
        return getPort().salesOrderShipmentInfo(getSessionId(), shipmentId);
    }

    public boolean addOrderShipmentComment(@NotNull String shipmentId,
                                           String comment,
                                           boolean sendEmail,
                                           boolean includeCommentInEmail) throws RemoteException
    {
        Validate.notNull(shipmentId);
        Validate.notNull(comment);
        return getPort().salesOrderShipmentAddComment(getSessionId(), shipmentId, comment,
            toIntegerString(sendEmail), toIntegerString(includeCommentInEmail));
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public List<Carrier> getOrderShipmentCarriers(@NotNull String orderId) throws RemoteException
    {
        Validate.notNull(orderId);
        return (List<Carrier>) CollectionUtils.collect(Arrays.asList(getPort().salesOrderShipmentGetCarriers(
            getSessionId(), orderId)), new Transformer()
        {
            public Object transform(Object input)
            {
                AssociativeEntity entity = (AssociativeEntity) input;
                return new Carrier(entity.getKey(), entity.getValue());
            }
        });
    }

    public int addOrderShipmentTrack(@NotNull String shipmentId,
                                     @NotNull String carrier,
                                     @NotNull String title,
                                     @NotNull String trackNumber) throws RemoteException
    {
        Validate.notNull(shipmentId);
        Validate.notNull(carrier);
        Validate.notNull(title);
        Validate.notNull(trackNumber);
        return getPort().salesOrderShipmentAddTrack(getSessionId(), shipmentId, carrier, title, trackNumber);
    }

    public boolean deleteOrderShipmentTrack(@NotNull String shipmentId, @NotNull String trackId)
        throws RemoteException
    {
        Validate.notNull(shipmentId);
        Validate.notNull(trackId);
        return getPort().salesOrderShipmentRemoveTrack(getSessionId(), shipmentId, trackId);
    }

    public String createOrderShipment(@NotNull String orderId,
                                      List<OrderItemIdQty> itemsQuantities,
                                      String comment,
                                      boolean sendEmail,
                                      boolean includeCommentInEmail) throws RemoteException
    {
        return getPort().salesOrderShipmentCreate(getSessionId(), orderId, itemsQuantities == null ? null : itemsQuantities.toArray(new OrderItemIdQty[itemsQuantities.size()]), comment,
                toInteger(sendEmail), toInteger(includeCommentInEmail));
    }

    public List<SalesOrderInvoiceEntity> listOrdersInvoices(String filter) throws RemoteException
    {
        return Arrays.asList(getPort().salesOrderInvoiceList(getSessionId(), FiltersParser.parse(filter)));
    }

    public SalesOrderInvoiceEntity getOrderInvoice(@NotNull String invoiceId) throws RemoteException
    {
        Validate.notNull(invoiceId);
        return getPort().salesOrderInvoiceInfo(getSessionId(), invoiceId);
    }

    public String createOrderInvoice(@NotNull String orderId,
                                     @NotNull List<OrderItemIdQty> itemsQuantities,
                                     String comment,
                                     boolean sendEmail,
                                     boolean includeCommentInEmail) throws RemoteException
    {
        Validate.notNull(orderId);
        return getPort().salesOrderInvoiceCreate(getSessionId(), orderId, itemsQuantities.toArray(new OrderItemIdQty[itemsQuantities.size()]), comment,
            toIntegerString(sendEmail), toIntegerString(includeCommentInEmail));
    }

    public boolean addOrderInvoiceComment(@NotNull String invoiceId,
                                          @NotNull String comment,
                                          boolean sendEmail,
                                          boolean includeCommentInEmail) throws RemoteException
    {
        Validate.notNull(invoiceId);
        Validate.notNull(comment);
        return getPort().salesOrderInvoiceAddComment(getSessionId(), invoiceId, comment, toIntegerString(sendEmail),
            toIntegerString(includeCommentInEmail));
    }

    public void captureOrderInvoice(@NotNull String invoiceId) throws RemoteException
    {
        Validate.notNull(invoiceId);
        getPort().salesOrderInvoiceCapture(getSessionId(), invoiceId);
    }

    public boolean voidOrderInvoice(@NotNull String invoiceId) throws RemoteException
    {
        Validate.notNull(invoiceId);
        return getPort().salesOrderInvoiceVoid(getSessionId(), invoiceId);
    }

    public void cancelOrderInvoice(@NotNull String invoiceId) throws RemoteException
    {
        Validate.notNull(invoiceId);
        getPort().salesOrderInvoiceCancel(getSessionId(), invoiceId);
    }

    private OrderItemIdQty[] fromMap(Map<Integer, Double> map)
    {
        OrderItemIdQty[] quantities = new OrderItemIdQty[map.size()];
        int i = 0;
        for (Entry<Integer, Double> entry : map.entrySet())
        {
            quantities[i] = new OrderItemIdQty(entry.getKey(), entry.getValue());
            i++;
        }
        return quantities;
    }

	@Override
	public void reorderOrder(@NotNull String incrementId, SalesOrderEntityToReorder customerData)
			throws RemoteException {
		Validate.notNull(incrementId);
		javax.xml.rpc.holders.StringHolder sh = new javax.xml.rpc.holders.StringHolder(incrementId);
		getPort().salesOrderReorder(getSessionId(), sh, customerData);
	}

}
