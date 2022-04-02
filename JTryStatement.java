// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a try-catch-finally statement.
 */
class JTryStatement extends JStatement {
    // The try block.
    private JBlock tryBlock;

    // The catch parameters.
    private ArrayList<JFormalParameter> parameters;

    // The catch blocks.
    private ArrayList<JBlock> catchBlocks;
    private ArrayList<JVariable> vars = new ArrayList<>();

    LocalContext context1;
    LocalContext context2;
    JBlock catchBlock;

    // The finally block.
    private JBlock finallyBlock;
//    private LocalContext context;
//    private LocalContext context2;

    /**
     * Constructs an AST node for a try-statement.
     *
     * @param line         line in which the while-statement occurs in the source file.
     * @param tryBlock     the try block.
     * @param parameters   the catch parameters.
     * @param catchBlocks  the catch blocks.
     * @param finallyBlock the finally block.
     */
    public JTryStatement(int line, JBlock tryBlock, ArrayList<JFormalParameter> parameters,
                         ArrayList<JBlock> catchBlocks, JBlock finallyBlock) {
        super(line);
        this.tryBlock = tryBlock;
        this.parameters = parameters;
        this.catchBlocks = catchBlocks;
        this.finallyBlock = finallyBlock;
    }

    /**
     * {@inheritDoc}
     */
    public JTryStatement analyze(Context contextparam) {
        //System.err.println(contextparam.names());
        tryBlock = tryBlock.analyze(contextparam);

        //analyze each catch block in a new localcontext created from conxtext as the parent
//        for(JBlock c: catchBlocks){
//             c.analyze(context);
//        }
        //for(int i = 0; i < catchBlocks.size(); i++){
            //System.out.println(init.get(i));

        //}
        JFormalParameter param;
        for(int i = 0; i < parameters.size(); i++){
            context1  = new LocalContext(contextparam);
           // System.out.println(parameters.get(i));
            param = parameters.get(i);
            parameters.set(i, (JFormalParameter) param.analyze(context1));

            JVariable v = new JVariable(param.line(), param.name());
            Type paramType = param.type().resolve(context1);//makes sure its not already defined in outer scope
            param.setType(paramType);

            LocalVariableDefn defn = new LocalVariableDefn(param.type(), context1.nextOffset());
            defn.initialize();
            context1.addEntry(param.line(), param.name(), defn);

            v = (JVariable) v.analyze(context1);
            //System.err.println(v.name() +" "+v.type());
            vars.add(v);

            catchBlock = catchBlocks.get(i);
            catchBlocks.set(i, catchBlock.analyze(context1));
        }

        //arraylist of jvariables(line, name)
        //for each codegen store

        //analyze finally block in a NEW localcontext created from context as the parent
        context2 = new LocalContext(contextparam);
        if(finallyBlock != null){
            finallyBlock = finallyBlock.analyze(context2);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        String startTry = output.createLabel();
        String startFinally = output.createLabel();
        String endFinally = output.createLabel();
        String endTry = output.createLabel();
        String endCatch = output.createLabel();
        String startFinallyPlusOne = output.createLabel();
        String startCatch = null;
        output.addLabel(startTry);

        tryBlock.codegen(output);
        if(finallyBlock != null){
            finallyBlock.codegen(output);
        }
        output.addBranchInstruction(GOTO, endFinally);
        output.addLabel(endTry);

        for(int i =0; i < catchBlocks.size(); i++){
            startCatch = output.createLabel();
            output.addLabel(startCatch);
            JVariable catchVar = vars.get(i);
            JBlock catchBlock  = catchBlocks.get(i);
            catchVar.codegenStore(output);
            //int offset = context1.offset();
            //output.addOneArgInstruction(ASTORE, 1);
            catchBlock.codegen(output);
            output.addLabel(endCatch);
            //add and exception handler with the appropriate variables
            //exception type not in internal form errors, so i found jvmName
            output.addExceptionHandler(startTry, endCatch, startCatch, catchVar.type().jvmName());
            //System.err.println(catchVar.name() +" "+catchVar.type().jvmName());
            if(finallyBlock != null){
                finallyBlock.codegen(output);
            }
            output.addBranchInstruction(GOTO, endFinally);
        }
        if(finallyBlock != null){
            output.addLabel(startFinally);
            int offset = context2.offset();
            //generate an ASTORE instruction with offset obtained from the context for the finally block
            output.addOneArgInstruction(ASTORE, offset);
            output.addLabel(startFinallyPlusOne);
            finallyBlock.codegen(output);
            //generate an ALOAD instruction with the offset
            output.addOneArgInstruction(ALOAD, offset);
            output.addNoArgInstruction(ATHROW);
            output.addLabel(endFinally);
            output.addExceptionHandler(startTry, endTry, startFinally, null);
            //for each catchblock, add an exception handler
            for(JBlock c :catchBlocks){
                output.addExceptionHandler(startCatch, endCatch, startFinally, null);
            }
            output.addExceptionHandler(startFinally,startFinallyPlusOne, startFinally, null);
//            output.addNoArgInstruction(ASTORE);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JTryStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("TryBlock", e1);
        tryBlock.toJSON(e1);
        if (catchBlocks != null) {
            for (int i = 0; i < catchBlocks.size(); i++) {
                JFormalParameter param = parameters.get(i);
                JBlock catchBlock = catchBlocks.get(i);
                JSONElement e2 = new JSONElement();
                e.addChild("CatchBlock", e2);
                String s = String.format("[\"%s\", \"%s\"]", param.name(), param.type() == null ?
                        "" : param.type().toString());
                e2.addAttribute("parameter", s);
                catchBlock.toJSON(e2);
            }
        }
        if (finallyBlock != null) {
            JSONElement e2 = new JSONElement();
            e.addChild("FinallyBlock", e2);
            finallyBlock.toJSON(e2);
        }
    }
}
