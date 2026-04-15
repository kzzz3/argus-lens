# Argus Wallet Payment Flow

## Product stance

Argus Stage 1 payment is a **wallet-to-wallet** system.

- There are only ordinary users.
- Every user owns one wallet.
- There is no merchant, cashier, store, or order object in the payment flow.

## Wallet page

The wallet page is the payment home for the app.

It must show:

- current wallet balance
- account id
- display name
- `Pay` action
- `Collect` action
- transfer history entry

## Pay flow

1. User opens `Wallet`.
2. User taps `Pay`.
3. App opens the QR scanner.
4. App resolves the scanned QR payload into a recipient wallet transfer session.
5. User reviews recipient, amount, and note.
6. User confirms transfer.
7. App shows the transfer result receipt.
8. User can continue scanning or open transfer history.

## Collect flow

1. User opens `Wallet`.
2. User taps `Collect`.
3. App renders the user's own QR code.
4. User may optionally prefill:
   - amount
   - note
5. Another user scans that QR code through their `Pay` flow.

## QR payload format

Current payload format:

`argus://pay?recipientAccountId=<accountId>&amount=<optional>&note=<optional>`

Rules:

- `recipientAccountId` is required.
- `amount` is optional.
- `note` is optional.
- If `amount` is absent, the payer must enter the transfer amount during confirmation.
- If `amount` is present, the transfer amount is fixed for that session.

## Receipt semantics

Receipts must reflect peer transfer semantics rather than the older checkout-style payment model.

Each receipt should preserve:

- payment id
- scan session id
- payer account id and display name
- recipient account id and display name
- amount
- currency
- note
- status
- processed time
- payer balance after transfer
- recipient balance after transfer

The UI should always render receipts from the viewer's perspective:

- `Sent` when the current user is the payer
- `Received` when the current user is the recipient

## History semantics

History is participant-based, not payer-only.

- A user should see transfers they sent.
- A user should also see transfers they received.
- Each history item should show direction plus counterparty.

## Development baseline

The current development backend initializes wallets lazily with:

- currency: `CNY`
- initial balance: `1000.00`

This is a development-stage convenience baseline, not a final production funding design.
