# WhileyRewriteLanguage
WyRL is a domain specific rewrite language and code generator which has been custom developed for use within the Whiley Compiler.  Specifically, WyRL is used to generate the Automated Theorem Prover used within Whiley.

# Building
To build WyRL, you can run ant from the command-line:

```
> ant

Buildfile: WhileyRewriteLanguage/build.xml

compile-wyrl:
    [javac] Compiling 1 source file
     [wyrl] Compiling 0 wyrl file(s)
    [javac] Compiling 5 source files

build:
    [mkdir] Created dir: WhileyRewriteLanguage/tmp
      [jar] Building jar: WhileyRewriteLanguage/lib/wyrl-v0.3.34.jar
   [delete] Deleting directory WhileyRewriteLanguage/tmp
     [echo] =============================================
     [echo] BUILT: lib/wyrl-v0.3.34.jar
     [echo] =============================================

BUILD SUCCESSFUL
Total time: 3 seconds
```

# Examples
There are several examples provided in the <code>examples/</code> directory:
<ul>

<li><p><b>Boolean Logic Simplifier.</b>  This simplifies formulae written in propositional logic.  For example, it would simplify <code>X||(Y||!Y)</code> to <code>X</code>.  For more, see <a href="examples/logic/README">here</a>.</p></li>

<li><p><b>Arithmetic Simplifier.</b>  This simplifies simple arithmetic expressions.  For example, it would simplify <code>y-y+(2*x)+(3*x)</code> to <code>5*x</code>.  For more, see <a href="examples/arithmetic/README">here</a>.</p></li>

<li><p><b>Recursive Types Simplifier.</b>  This simplifies types involving unions, intersections and negations and which may also be recursive.  For example, it would simplify <code>(int|!int)&int</code> to <code>int</code>.  For more, see <a href="examples/types/README">here</a>.</p></li>

<li><p><b>Transitive Closure.</b>  This applies transitive closure to strict inequalities.  For example, it would rewrite <code>x < y,y < z</code> to <code>x < y, y < z, x < z</code>.  For more, see <a href="examples/closure/README">here</a>.</p></li>

<li><p><b>Quantifier Instantiater.</b>  This instantiates and simplifies boolean expressions involving quantifiers.  For example, it would simplify <code>fn(x,y) && forall X.!fn(X,X)</code> to <code>False</code>.  For more, see <a href="examples/quantifiers/README">here</a>.</p></li>

</ul>