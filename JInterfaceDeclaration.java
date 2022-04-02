// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import com.sun.jdi.InterfaceType;

import java.util.ArrayList;

/**
 * A representation of an interface declaration.
 */
class JInterfaceDeclaration extends JAST implements JTypeDecl {
    // Interface modifiers.
    private ArrayList<String> mods;

    // Interface name.
    private String name;

    // This interface type.
    private Type thisType;

    // Super class type.
    private Type superType;

    // Extended interfaces.
    private ArrayList<TypeName> superInterfaces;

    // Interface block.
    private ArrayList<JMember> interfaceBlock;

    // Context for this interface.
    private ClassContext context;

    /**
     * Constructs an AST node for an interface declaration.
     *
     * @param line            line in which the interface declaration occurs in the source file.
     * @param mods            class modifiers.
     * @param name            class name.
     * @param superInterfaces super class types.
     * @param interfaceBlock  interface block.
     */
    public JInterfaceDeclaration(int line, ArrayList<String> mods, String name,
                                 ArrayList<TypeName> superInterfaces,
                                 ArrayList<JMember> interfaceBlock) {
        super(line);
        this.mods = mods;
        this.name = name;
        this.superType = Type.OBJECT;
        this.superInterfaces = superInterfaces;
        this.interfaceBlock = interfaceBlock;

        // An interface must have the "abstract" and "interface" modifiers.
        if (!this.mods.contains("abstract")) {
            mods.add("abstract");
        }
        this.mods.add("interface");
    }

    /**
     * {@inheritDoc}
     */
    public void declareThisType(Context context) {
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        CLEmitter partial = new CLEmitter(false);
        partial.addClass(mods, qualifiedName, Type.OBJECT.jvmName(), null, false);//superinterfaces shoul dbe a arratylist of strings
        thisType = Type.typeFor(partial.toClass());
        context.addType(line, thisType);
    }

    /**
     * {@inheritDoc}
     */
    public void preAnalyze(Context context) {// Construct a class context.
        this.context = new ClassContext(this, context);

        // Resolve superclass.
        superType = superType.resolve(this.context);

        // Creating a partial class in memory can result in a java.lang.VerifyError if the
        // semantics below are violated, so we can't defer these checks to analyze().
        thisType.checkAccess(line, superType);
        if (superType.isFinal()) {
            JAST.compilationUnit.reportSemanticError(line, "Cannot extend a final type: %s",
                    superType.toString());
        }

        // Create the (partial) class.
        CLEmitter partial = new CLEmitter(false);

        // Add the class header to the partial class
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        partial.addClass(mods, qualifiedName, superType.jvmName(), null, false);

        // Pre-analyze the members and add them to the partial class.
        for (JMember member : interfaceBlock) {
            member.preAnalyze(this.context, partial);
//            hasExplicitConstructor =
//                    hasExplicitConstructor || member instanceof JConstructorDeclaration;
        }

//        // Add the implicit empty constructor?
//        if (!hasExplicitConstructor) {
//            codegenPartialImplicitConstructor(partial);
//        }

        // Get the ClassRep for the (partial) class and make it the representation for this type.
        Type id = this.context.lookupType(name);
        if (id != null && !JAST.compilationUnit.errorHasOccurred()) {
            id.setClassRep(partial.toClass());
        }
    }

    /**
     * {@inheritDoc}
     */
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Type superType() {
        return superType;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList<TypeName> superInterfaces() {
        return superInterfaces;
    }

    /**
     * {@inheritDoc}
     */
    public Type thisType() {
        return thisType;
        //return null;
    }

    /**
     * {@inheritDoc}
     */
    public JAST analyze(Context context) {
        // Analyze all members.
        for (JMember member : interfaceBlock) {
            ((JAST) member).analyze(this.context);
        }

        // Separate declared fields for purposes of initialization.
        for (JMember member : interfaceBlock) {
            if (member instanceof JFieldDeclaration) {
                JFieldDeclaration fieldDecl = (JFieldDeclaration) member;
//                if (fieldDecl.mods().contains("static")) {
//                    staticFieldInitializations.add(fieldDecl);
//                } else {
//                    instanceFieldInitializations.add(fieldDecl);
//                }
            }
        }

        // Finally, ensure that a non-abstract class has no abstract methods.
        if (!thisType.isAbstract() && thisType.abstractMethods().size() > 0) {
            String methods = "";
            for (Method method : thisType.abstractMethods()) {
                methods += "\n" + method;
            }
            JAST.compilationUnit.reportSemanticError(line,
                    "Interface must be abstract since it defines Interface methods: %s", methods);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        //include a list of implemented interfaces in the class header
        ArrayList <String> str = new ArrayList<>();
        // The class header.
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        if(superInterfaces != null){
            for(TypeName i : superInterfaces){
                String name  = i.jvmName();
                str.add(name);
            }
            output.addClass(mods, qualifiedName, superType.jvmName(), str, false);
        }
        else{
            //superInterfaces.add(Type.OBJECT);
            //add class for type.object, if there are no other interfaces
            output.addClass(mods, qualifiedName, superType.jvmName(), null, false);
        }

        // The implicit empty constructor?
//        if (!hasExplicitConstructor) {
//            codegenImplicitConstructor(output);
//        }

        // The members.
        if(interfaceBlock != null){
            for (JMember member : interfaceBlock) {
                ((JAST) member).codegen(output);
            }
        }


        // Generate a class initialization method?
//        if (staticFieldInitializations.size() > 0) {
//            codegenClassInit(output);
//        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JInterfaceDeclaration:" + line, e);
        if (mods != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (String mod : mods) {
                value.add(String.format("\"%s\"", mod));
            }
            e.addAttribute("modifiers", value);
        }
        e.addAttribute("name", name);
        e.addAttribute("super", superType == null ? "" : superType.toString());
        if (superInterfaces != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (TypeName impl : superInterfaces) {
                value.add(String.format("\"%s\"", impl.toString()));
            }
            e.addAttribute("extends", value);
        }
        if (context != null) {
            context.toJSON(e);
        }
        if (interfaceBlock != null) {
            for (JMember member : interfaceBlock) {
                ((JAST) member).toJSON(e);
            }
        }
    }
}
