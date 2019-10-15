package core

import application.{WordResolveService, WordResolveServiceImpl}
import com.softwaremill.macwire._
import controllers.{
  HomeController,
  SampleBindableController,
  SampleController,
  SearchController,
  WordController
}
import dbport.IOContextSupport.DBTask
import dbport.{IOContextManagerOnJDBC, WordRepositoryOnJDBC}
import domain.{IOContextManager, WordRepository, WordResolveUseCase, WordResolveUseCaseImpl}
import monix.eval.Task
import play.api.mvc.ControllerComponents
import scalikejdbc.DBSession

trait Components {

  lazy val ioContextManagerOnJDBC: IOContextManager[Task, DBSession] = wire[IOContextManagerOnJDBC]
  lazy val wordRepository: WordRepository[DBTask] = wire[WordRepositoryOnJDBC]
  lazy val wordResolveUseCase: WordResolveUseCase[DBTask] = wire[WordResolveUseCaseImpl[DBTask]]
  lazy val wordResolveService: WordResolveService[Task] =
    wire[WordResolveServiceImpl[Task, DBSession]]

  def controllerComponents: ControllerComponents
  lazy val wordController: WordController = wire[WordController]
  lazy val homeController: HomeController = wire[HomeController]
  lazy val sampleController: SampleController = wire[SampleController]
  lazy val sampleBindableController: SampleBindableController = wire[SampleBindableController]
  lazy val searchController: SearchController = wire[SearchController]
}
