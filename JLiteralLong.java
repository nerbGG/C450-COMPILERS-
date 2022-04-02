// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a long literal.
 */
class JLiteralLong extends JExpression {
    // String representation of the literal.
    private String text;

    /**
     * Constructs an AST node for a long literal given its line number and string representation.
     *
     * @param line line in which the literal occurs in the source file.
     * @param text string representation of the literal.
     */
    public JLiteralLong(int line, String text) {
        super(line);
        this.text = text;
    }
    /**
     * Returns the literal as a long.
     *
     * @return the literal as a long.
     */
    public long toLong() {
//        System.err.println(text);
        return Long.parseLong(text.substring(0,text.length()-1));
    }
    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        type = Type.LONG;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) { //james helped with this
        long i = toLong(); //having trouble parsing a long in the form 1L
       // String i  =
        if(i == 0) {
            output.addNoArgInstruction(LCONST_0);
        }
        else if(i == 1) {
            output.addNoArgInstruction(LCONST_1);
        }
        else
            output.addLDCInstruction(i);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JLiteralLong:" + line, e);
        e.addAttribute("type", type == null ? "" : type.toString());
        e.addAttribute("value", text);
    }
}
