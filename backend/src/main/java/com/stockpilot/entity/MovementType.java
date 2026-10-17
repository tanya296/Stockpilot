package com.stockpilot.entity;

/** Why a stock movement happened. Every change to a StockLevel writes one of these. */
public enum MovementType {
    IN,            // manual stock in (e.g. a count correction, a return)
    OUT,           // manual stock out (e.g. used in a repair, damaged)
    TRANSFER_IN,   // arrived from another warehouse
    TRANSFER_OUT,  // sent to another warehouse
    PO_RECEIPT     // received from a supplier purchase order
}
