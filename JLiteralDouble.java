// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a double literal.
 */
class JLiteralDouble extends JExpression {
    // String representation of the literal.
    private String text;

    /**
     * Constructs an AST node for a double literal given its line number and string representation.
     *
     * @param line line in which the literal occurs in the source file.
     * @param text string representation of the literal.
     */
    public JLiteralDouble(int line, String text) {
        super(line);
        this.text = text;
    }
    /**
     * Returns the literal as a double.
     *
     * @return the literal as a double.
     */
    public double toDouble() {
        return Double.parseDouble(text);
    }
    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        type  = Type.DOUBLE;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        double i = toDouble();
        if(i == 0.0 || i == 0.0D  || i == 0.0d ) {//james helped with this
            output.addNoArgInstruction(DCONST_0);
        }
        else if(i == 1.0 || i == 1.0D || i == 1.0d) {
            output.addNoArgInstruction(DCONST_1);
        }
        else
            output.addLDCInstruction(i);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JLiteralDouble:" + line, e);
        e.addAttribute("type", type == null ? "" : type.toString());
        e.addAttribute("value", text);
    }
}
