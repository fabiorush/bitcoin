import java.util.ArrayList;
import java.util.Arrays;

public class MaxFeeTxHandler {

	private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
    	this.utxoPool = new UTXOPool(utxoPool);
    }
    
    private double validateOutput(Transaction tx) {
    	ArrayList<Transaction.Output> outputs = tx.getOutputs();
		double outputValue = 0;

    	for (int i = 0; i < outputs.size(); i++) {
    		Transaction.Output output = outputs.get(i);

    		/* (4) all of {@code tx}s output values are non-negative, and */
    		if (output.value < 0)
    			return null;
    		
    		outputValue += output.value;
    		
    		UTXO newOut = new UTXO(tx.getHash(), i);
    		utxoPoolLocal.addUTXO(newOut, output);
    	}

    }

    private UTXOPool validateTx(Transaction tx) {
    	ArrayList<Transaction.Input> inputs = tx.getInputs();
    	ArrayList<Transaction.Output> outputs = tx.getOutputs();
		UTXOPool utxoPoolLocal = new UTXOPool(utxoPool);
		double inputValue = 0;
		double outputValue = 0;

    	for (int i = 0; i < inputs.size(); i++) {
    		Transaction.Input input = inputs.get(i);

    	    /* (1) all outputs claimed by {@code tx} are in the current UTXO pool */ 
    		UTXO newOut = new UTXO(input.prevTxHash, input.outputIndex);
    		if (!utxoPoolLocal.contains(newOut))
    			return null;
    		
    	    /* (2) the signatures on each input of {@code tx} are valid, */ 
    		Transaction.Output output = utxoPoolLocal.getTxOutput(newOut);
    		byte[] rawdata = tx.getRawDataToSign(i);
			if (!Crypto.verifySignature(output.address, rawdata, input.signature))
				return null;

			inputValue += output.value;

			/* (3) no UTXO is claimed multiple times by {@code tx}, */
			utxoPoolLocal.removeUTXO(newOut);
    	}
    	
    	for (int i = 0; i < outputs.size(); i++) {
    		Transaction.Output output = outputs.get(i);

    		/* (4) all of {@code tx}s output values are non-negative, and */
    		if (output.value < 0)
    			return null;
    		
    		outputValue += output.value;
    		
    		UTXO newOut = new UTXO(tx.getHash(), i);
    		utxoPoolLocal.addUTXO(newOut, output);
    	}
	    /* (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
	     *     values; and false otherwise. */
    	if (inputValue < outputValue)
    		return null;

    	return utxoPoolLocal;
    }
    	
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

    	if (validateTx(tx) == null)
    		return false;

    	return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	for (int i = 0; i < possibleTxs.length; i++) {
    		UTXOPool utxoPoolLocal = validateTx(possibleTxs[i]);
    		if (utxoPoolLocal != null) {
    			utxoPool = utxoPoolLocal;
    			currentTxs.add(possibleTxs[i]);
    		}
    	}
    	
        return (Transaction[]) currentTxs.toArray();
    }
}
