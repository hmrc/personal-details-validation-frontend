import sbt.{ForkOptions, TestDefinition}
import sbt.Tests.{Group, SubProcess}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] = {
    val browserProperty = Option(System.getProperty("browser","firefox")).map(browser => s"-Dbrowser=$browser")
    tests map {
      test => Group(
        test.name,
        Seq(test),
        SubProcess(ForkOptions(runJVMOptions = browserProperty.getOrElse("") :: s"-Dtest.name=${test.name}" :: Nil))
      )
    }
  }
}