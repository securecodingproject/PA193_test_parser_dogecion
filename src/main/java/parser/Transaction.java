package parser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

// https://en.bitcoin.it/wiki/Transaction
public class Transaction {
    public int version;
    public VarInt inCounter;
    public List<TransactionInput> inputs;
    public VarInt outCounter;
    public List<TransactionOutput> outputs;
    public static int lockTimeLength = 4;
    public byte[] lockTime;
    public int transactionSize;

    public Transaction() {
        lockTime = new byte[lockTimeLength];
    }

    public static Transaction parseFirstTransactionFromBytes(byte[] transactionListBytes) {
        if (transactionListBytes == null)
            return null;

        ByteBuffer txBuffer = ByteBuffer.wrap(transactionListBytes);
        txBuffer.order(ByteOrder.LITTLE_ENDIAN);

        Transaction tx = new Transaction();
        tx.version = txBuffer.getInt();

        byte[] vi = new byte[9];
        txBuffer.get(vi);
        tx.inCounter = new VarInt(vi, 0);
        // jump to after varint
        txBuffer.position(txBuffer.position() - (9 - tx.inCounter.size));

        List<TransactionInput> txInList = new ArrayList<TransactionInput>();
        int remainingNumberOfBytesOfTxBuffer = txBuffer.limit() - txBuffer.position();
        long remainingTxInToParse = tx.outCounter.value;
        for (long i = remainingTxInToParse; i > 0; i--) {
            int initialPos = txBuffer.position();

            // get remaining bytes
            byte[] remainingBytes = new byte[remainingNumberOfBytesOfTxBuffer];
            txBuffer.get(remainingBytes);

            TransactionInput txIn = TransactionInput.parseTransactionInput(remainingBytes);
            if (txIn == null)
                return null;

            txInList.add(txIn);

            remainingNumberOfBytesOfTxBuffer -= txIn.inputSize;

            txBuffer.position(initialPos + (int)txIn.inputSize);
        }

        // sum txIn lengths
        int sumTxInLength = 0;
        for (TransactionInput txIn : tx.inputs)
            sumTxInLength += txIn.inputSize;

        txBuffer.get(vi);
        tx.outCounter = new VarInt(vi, 0);
        // jump to after varint
        txBuffer.position(txBuffer.position() - (9 - tx.outCounter.size));

        List<TransactionOutput> txOutList = new ArrayList<TransactionOutput>();
        remainingNumberOfBytesOfTxBuffer = txBuffer.limit() - txBuffer.position();
        long remainingTxOutToParse = tx.outCounter.value;
        for (long i = remainingTxOutToParse; i > 0; i--) {
            int initialPos = txBuffer.position();

            // get remaining bytes
            byte[] remainingBytes = new byte[remainingNumberOfBytesOfTxBuffer];
            txBuffer.get(remainingBytes);

            TransactionOutput txOut = TransactionOutput.parseTransactionOutput(remainingBytes);
            if (txOut == null)
                return null;

            txOutList.add(txOut);

            remainingNumberOfBytesOfTxBuffer -= txOut.outputSize;

            txBuffer.position(initialPos + (int)txOut.outputSize);
        }

        // sum txOut lengths
        int sumTxOutLength = 0;
        for (TransactionOutput txOut : tx.outputs)
            sumTxOutLength += txOut.outputSize;

        byte[] lockTimeBytes = new byte[lockTimeLength];
        txBuffer.get(lockTimeBytes);

        tx.transactionSize = 4 + tx.inCounter.size + tx.outCounter.size + sumTxInLength + sumTxOutLength + lockTimeLength;

        return tx;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Version: ");
        sb.append(Integer.toUnsignedString(this.version));

        sb.append("\r\nNumber of transaction inputs: ");
        sb.append(Long.toUnsignedString(this.outCounter.value));
        sb.append("\r\nTransaction inputs:\r\n");
        for (TransactionInput txIn : this.inputs) {
            sb.append("\t");
            sb.append(txIn.toString());
            sb.append("\r\n");
        }

        sb.append("\r\nNumber of transaction outputs: ");
        sb.append(Long.toUnsignedString(this.outCounter.value));
        sb.append("\r\nTransaction outputs:\r\n");
        for (TransactionOutput txOut : this.outputs) {
            sb.append("\t");
            sb.append(txOut.toString());
            sb.append("\r\n");
        }

        sb.append(Helpers.byteArrayToHexString(this.lockTime));

        return sb.toString();
    }
}
