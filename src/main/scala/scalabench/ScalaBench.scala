package scalabench

import java.io.File
import java.io.FileWriter
import javax.tools.ToolProvider

object ScalaBench {
  val numberOfClasses = 5000
  val targetDir = {
    val d = new File("target/generatedSrcs")
    d.delete()
    d.mkdirs()
    d
  }
  val javaTargetDir = {
    val d = new File(targetDir, "java")
    d.mkdir()
    d
  }
  val scalaTargetDir = {
    val d = new File(targetDir, "scala")
    d.mkdir()
    d
  }
  val targetClasses = {
    val d = new File(targetDir, "classes")
    d.delete()
    d.mkdir()
    d
  }

  def generateJavaClasses: Map[String, String] = {
    def generateClass(i: Int): String =
      s"""|
          |package javaBench;
          |public class C$i { }""".stripMargin
    val pairs = for (i <- 1 to numberOfClasses) yield {
      s"C$i.java" -> generateClass(i)
    }
    Map(pairs: _*)
  }

  def generateScalaClasses: Map[String, String] = {
    def generateClass(i: Int): String =
      s"""|
          |package scalaBench
          |class C$i { }""".stripMargin
    val pairs = for (i <- 1 to numberOfClasses) yield {
      s"C$i.scala" -> generateClass(i)
    }
    Map(pairs: _*)
  }

  def writeSourceFiles(into: File, srcs: Map[String, String]): Seq[File] = {
    assert(into.isDirectory, s"The $into is not a directory!")
    val srcFiles = for ((srcFileName, src) <- srcs.toSeq) yield {
      val srcFile = new File(into, srcFileName)
      srcFile.createNewFile()
      val writer = new FileWriter(srcFile)
      writer.write(src)
      writer.close()
      srcFile
    }
    srcFiles
  }

  def writeJava: Seq[File] = {
    val classes = generateJavaClasses
    writeSourceFiles(javaTargetDir, classes)
  }

  def writeScala: Seq[File] = {
    val classes = generateScalaClasses
    writeSourceFiles(scalaTargetDir, classes)
  }

  private def scalaArgs(stopBeforePhaseName: String, srcFiles: Seq[File]): Seq[String] =
    Seq("-usejavacp", "-d", targetClasses.getAbsolutePath, s"-Ystop-before:$stopBeforePhaseName") ++ srcFiles.map(_.getAbsoluteFile.toString)

  def compileScala(srcFiles: Seq[File]): Unit = {
    val main = scala.tools.nsc.Main
    val args = scalaArgs("patmat", srcFiles)
    //println(s"scalac args: $args")
    main.process(args.toArray)
  }

  def compileDotty(srcFiles: Seq[File]): Unit = {
    val main = dotty.tools.dotc.Main
    val args = scalaArgs("superaccessors", srcFiles)
    //println(s"scalac args: $args")
    import dotty.tools.dotc.core.Contexts
    val initCtx = (new Contexts.ContextBase).initialCtx
    main.process(args.toArray, initCtx)
  }

  def compileJava(srcFiles: Seq[File]): Unit = {
    import scala.collection.JavaConverters._
    val compiler = ToolProvider.getSystemJavaCompiler()
    val stdFileManager = compiler.getStandardFileManager(null, null, null)
    val compilationUnits = stdFileManager.getJavaFileObjectsFromFiles(srcFiles.asJava);
    val options: List[String] = Nil
    compiler.getTask(null, stdFileManager, null, options.asJava, null, compilationUnits).call();
  }

  def measureTime(name: String)(action: => Unit): Unit = {
    val startTime = System.currentTimeMillis()
    action
    val elapsed = System.currentTimeMillis() - startTime
    println(s"[$name] took $elapsed ms")
  }

  def main(args: Array[String]): Unit = {
    val javaFiles = writeJava
    val scalaFiles = writeScala
    //measureTime("javac") { compileJava(javaFiles) }
    //measureTime("dottyc") { compileDotty(scalaFiles) }
    measureTime("scalac") { compileScala(scalaFiles) }

    println("done")
  }
}