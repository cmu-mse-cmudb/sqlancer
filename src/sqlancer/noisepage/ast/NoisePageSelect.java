package sqlancer.noisepage.ast;

import sqlancer.ast.SelectBase;
import sqlancer.ast.newast.Node;

public class NoisePageSelect extends SelectBase<Node<NoisePageExpression>> implements Node<NoisePageExpression> {

    private boolean isDistinct;

    public void setDistinct(boolean isDistinct) {
        this.isDistinct = isDistinct;
    }

    public boolean isDistinct() {
        return isDistinct;
    }

}
