package controllers

import com.synaptix.toast.dao.domain.impl.report.{Project, Campaign}
import com.synaptix.toast.dao.domain.impl.test.TestPage
import com.synaptix.toast.runtime.parse.TestParser
import controllers.mongo.Scenario
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{Json, JsError}
import play.api.mvc.{ResponseHeader, SimpleResult, Action, Controller}
import toast.engine.ToastRuntimeJavaWrapper
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.immutable.StringOps
import com.synaptix.toast.runtime.report.HTMLReporter

case class ScenarioWrapper(id: Option[String], name: Option[String], scenario: Option[Scenario])
case class Cpgn(id: Option[String], name: String, scenarii: List[ScenarioWrapper])
case class Prj(id: Option[String], name: String, iterations: Option[Short], campaigns: List[Cpgn])

object ProjectController  extends Controller {
  lazy val projectJavaDaoService = ToastRuntimeJavaWrapper.projectService
  implicit val sFormat = Json.format[ScenarioWrapper]
  implicit val campaignFormat = Json.format[Cpgn]
  implicit val projectFormat = Json.format[Prj]

  /**
   * load to init projects
   */
  def loadProject() = Action {
    val projects = projectJavaDaoService.findAllReferenceProjects().iterator
    var prjs = List[Prj]()
    while (projects.hasNext()) {
      val project = projects.next()
      var cmpgs = List[Cpgn]()
      val campaigns = project.getCampaigns().iterator
      while (campaigns.hasNext()) {
        val campaign = campaigns.next()
        var scns = List[ScenarioWrapper]()
        val scenarii = campaign.getTestCases().iterator
        while (scenarii.hasNext()) {
          val scenario = scenarii.next()
          scns = ScenarioWrapper(Some(scenario.getIdAsString()),Some(scenario.getPageName()), None) :: scns
        }
        cmpgs = Cpgn(Some(campaign.getIdAsString()), campaign.getName(), scns.reverse) :: cmpgs
      }
      prjs = Prj(Some(project.getId().toString()), project.getName(), Some(project.getIteration()) , cmpgs) :: prjs
    }

    Ok(Json.toJson(prjs))
  }

  /**
   * Save project
   */
  def saveProject() = Action(parse.json) { implicit request =>
    val parser = new TestParser()

    def parseTestPage(scenario: Scenario, wikiScenario: String): TestPage = {
      val testPage = parser.parseString(wikiScenario)
      scenario.id match {
        case None => {}
        case Some(id) => testPage.setId(id)
      }
      testPage.setName(scenario.name)
      testPage.setPageName(scenario.name)
      testPage
    }

    def transformCampaign(campaigns: List[Cpgn]): java.util.ArrayList[Campaign] = {
      val list = new java.util.ArrayList[Campaign]()
      for (cpgn <- campaigns) {
        val campaign = new Campaign()
        campaign.setName(cpgn.name)
        val testPagelist = new java.util.ArrayList[TestPage]()
        val testPages = (for (c <- campaigns; wrapper <- c.scenarii) yield parseTestPage(wrapper.scenario.get, ScenarioController.wikifiedScenario(wrapper.scenario.get).as[String]))
        for (tPage <- testPages) {
          testPagelist.add(tPage)
        }
        campaign.setTestCasesImpl(testPagelist)
        list.add(campaign)
      }
      list
    }
    def tranformProject(p: Prj): Project = {
      val project = new Project()
      project.setName(p.name)
      project.setCampaignsImpl(transformCampaign(p.campaigns))
      project
    }

    request.body.validate[Prj].map {
      case project: Prj => {
        projectJavaDaoService.saveReferenceProject(tranformProject(project))
        Ok("project saved !") 
      }
    }.recoverTotal {
      e => BadRequest("Detected error:" + JsError.toFlatJson(e))
    }
  }

  def loadProjectReport(name: String) = Action {
    val report = HTMLReporter.getProjectHTMLReport(name)
    SimpleResult(header = ResponseHeader(200, Map(CONTENT_TYPE -> "text/html")),
                  body = Enumerator(new StringOps(report).getBytes()))
  }

  def loadTestReport() = Action {
    implicit request => {
      val pName = request.queryString("project")(0);
      val iter = request.queryString("iteration")(0);
      val tName = request.queryString("test")(0);
      var p = projectJavaDaoService.getByNameAndIteration(pName, iter);
      var pageReport = ""
      val iteratorCampaign = p.getCampaigns().iterator
      while (iteratorCampaign.hasNext()) {
        val iteratorTestPage = iteratorCampaign.next().getTestCases().iterator
        while(iteratorTestPage.hasNext()){
          val testPage = iteratorTestPage.next()
          if (testPage.getName().equals(tName)) {
            pageReport = HTMLReporter.getTestPageHTMLReport(testPage);
          }
        }
      }
      SimpleResult( header = ResponseHeader(200, Map(CONTENT_TYPE -> "text/html")),
        body = Enumerator(new StringOps(pageReport).getBytes()))
    }
  }
}