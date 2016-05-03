package actors;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.firebase.client.*;
import com.firebase.security.token.TokenGenerator;
import com.firebase.security.token.TokenOptions;
import com.google.common.collect.ImmutableList;
import models.*;
import models.Document;
import org.apache.poi.xwpf.usermodel.*;
import play.libs.ws.WSRequest;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.runtime.BoxedUnit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ReportGeneratorActor extends AbstractActor {

    public static Props props() {
        return Props.create(ReportGeneratorActor.class);
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final DateFormat df = new SimpleDateFormat("d.MM hh:mm:ss");

    public ReportGeneratorActor() {
        Scheduler scheduler = getContext().system().scheduler();
        scheduler.scheduleOnce(Duration.create(5, TimeUnit.MINUTES),
                self(), "timeout", context().dispatcher(), null);

        receive(initialState());
    }

    final PartialFunction<Object, BoxedUnit> initialState() {
        return ReceiveBuilder.
                match(ReportGeneratorActorProtocol.GenerateReportlXml.class, msg -> {
                    processGenerateReportXml(msg);
                })
                .matchEquals("timeout", msg -> {
                    this.terminateSilently();
                }).build();
    }

    final PartialFunction<Object, BoxedUnit> waitingForFirebaseDocument(final ActorRef parent, WSClient ws, String firebaseSecret, String filepickerSecret) {
        return ReceiveBuilder.
                match(ReportGeneratorActorProtocol.RecievedDocument.class, msg -> {
                    this.processReceivedDocument(msg, parent, ws, firebaseSecret, filepickerSecret);
                })
                .match(ReportGeneratorActorProtocol.RecievedError.class, msg -> {
                    this.processReceivedError(msg, parent);
                })
                .matchEquals("timeout", msg -> {
                    this.terminateWithFailureResponse(parent, msg);
                }).build();
    }

    final PartialFunction<Object, BoxedUnit> waitingForFilepickerDocument(final ActorRef parent, final Document document, WSClient ws, String firebaseSecret, String filepickerSecret) {
        return ReceiveBuilder.
                match(ReportGeneratorActorProtocol.RecievedFile.class, msg -> {
                    this.processReceivedFile(msg, parent, ws, firebaseSecret, filepickerSecret);
                })
                .match(ReportGeneratorActorProtocol.RecievedError.class, msg -> {
                    this.processReceivedError(msg, parent);
                })
                .matchEquals("timeout", msg -> {
                    this.terminateWithFailureResponse(parent, msg);
                }).build();
    }

    final PartialFunction<Object, BoxedUnit> parsing_waitingForHeader(
            final ActorRef parent,
            final Document document,
            WSClient ws,
            String firebaseSecret,
            String filepickerSecret,
            ImmutableList<TableMapping.TableCreator> creators,
            ImmutableList<TableMapping> mappers) {
        return ReceiveBuilder.
                match(ReportGeneratorActorProtocol.ParagraphFinded.class, msg -> {
                    Optional<IBodyElement> currentIBodyElementOpt = msg.rest.findFirst();
                    if (currentIBodyElementOpt.isPresent()) {
                        IBodyElement currentIBodyElement = currentIBodyElementOpt.get();
                        if (currentIBodyElement instanceof XWPFParagraph ) {
                            XWPFParagraph current = (XWPFParagraph)currentIBodyElement;
                            Optional<TableMapping> matchedmapperopt = mappers
                                    .stream()
                                    .filter(i -> current.searchText(i.searchInPlan, new PositionInParagraph()) != null)
                                    .findFirst();
                            if (currentIBodyElementOpt.isPresent()) {
                                this.context().become(
                                        this.parsing_waitingForTable(parent,
                                                document,
                                                ws,
                                                firebaseSecret,
                                                filepickerSecret,
                                                creators,
                                                mappers,
                                                matchedmapperopt.get()
                                        )
                                );
                            }
                        }
                        this.self().tell(new ReportGeneratorActorProtocol.ParagraphFinded(msg.rest.skip(1)), this.self());
                    } else {
                        this.self().tell(new ReportGeneratorActorProtocol.ParsingEnded(), this.self());
                    }
                })
                .match(ReportGeneratorActorProtocol.ParsingEnded.class, msg -> {
                    // todo
                })
                .match(ReportGeneratorActorProtocol.RecievedError.class, msg -> {
                    this.processReceivedError(msg, parent);
                })
                .matchEquals("timeout", msg -> {
                    this.terminateWithFailureResponse(parent, msg);
                }).build();
    }

    final PartialFunction<Object, BoxedUnit> parsing_waitingForTable(
            final ActorRef parent,
            final Document document,
            WSClient ws,
            String firebaseSecret,
            String filepickerSecret,
            ImmutableList<TableMapping.TableCreator> creators,
            ImmutableList<TableMapping> mappers,
            TableMapping currentmapping) {
        return ReceiveBuilder.
                match(ReportGeneratorActorProtocol.ParagraphFinded.class, msg -> {
                    Optional<IBodyElement> currentIBodyElementOpt = msg.rest.findFirst();
                    if (currentIBodyElementOpt.isPresent()) {
                        IBodyElement currentIBodyElement = currentIBodyElementOpt.get();
                        if (currentIBodyElement instanceof XWPFTable) {
                            XWPFTable current = (XWPFTable)currentIBodyElement;
                            // todo
                        }
                        this.self().tell(new ReportGeneratorActorProtocol.ParagraphFinded(msg.rest.skip(1)), this.self());
                    } else {
                        this.self().tell(new ReportGeneratorActorProtocol.ParsingEnded(), this.self());
                    }
                })
                .match(ReportGeneratorActorProtocol.ParsingEnded.class, msg -> {
                    // todo
                })
                .match(ReportGeneratorActorProtocol.RecievedError.class, msg -> {
                    this.processReceivedError(msg, parent);
                })
                .matchEquals("timeout", msg -> {
                    this.terminateWithFailureResponse(parent, msg);
                }).build();
    }

    private void processGenerateReportXml(ReportGeneratorActorProtocol.GenerateReportlXml msg) {
        Firebase rootRef = new Firebase(msg.firebaseUrl);
        //rootRef.authWithCustomToken();
        final ActorRef self_c = this.self();
        rootRef.authWithCustomToken(this.getToken(msg.firebaseSecret), new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {

            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                self_c.tell(new ReportGeneratorActorProtocol.RecievedError("auth failed"), self_c);
            }
        });

        rootRef.child("documents")
                .child(msg.documentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot querySnapshot) {
                        Document item = querySnapshot.getValue(Document.class);
                        item.setKey(querySnapshot.getKey());
                        self_c.tell(new ReportGeneratorActorProtocol.RecievedDocument(item), self_c);
                    }
                    @Override
                    public void onCancelled(FirebaseError error) {
                        log.error("Error while retrieving course-dates: %s", error.toString());
                        self_c.tell(new ReportGeneratorActorProtocol.RecievedError(error.toString()), self_c);
                    }
                });

        getContext().become(waitingForFirebaseDocument(this.sender(), msg.ws, msg.firebaseSecret, msg.filepickerSecret));
    }

    private String getToken(String firebaseSecret) {
        Map<String, Object> authPayload = new HashMap<String, Object>();
        authPayload.put("uid", "1");

        TokenOptions tokenOptions = new TokenOptions();
        tokenOptions.setAdmin(true);

        TokenGenerator tokenGenerator = new TokenGenerator(firebaseSecret);
        String token = tokenGenerator.createToken(authPayload, tokenOptions);
        return token;
    }

    private void processReceivedError(ReportGeneratorActorProtocol.RecievedError msg, ActorRef parent) {
        terminateWithFailureResponse(parent, msg.msg);
    }

    private void processReceivedDocument(ReportGeneratorActorProtocol.RecievedDocument msg, ActorRef parent, WSClient ws, String firebaseSecret, String filepickerSecret) {
        final String filename = msg.document.getFpfile().getFilename();
        final Boolean isFilenameGood = filename.endsWith(".doc") || filename.endsWith(".docx");
        if (isFilenameGood) {
            final WSRequest request = ws.url(msg.document.getFpfile().getUrl());
            request.get().thenApply(WSResponse::asByteArray).thenApply(response -> {
                this.self().tell(new ReportGeneratorActorProtocol.RecievedFile(response), this.self());
                return response;
            });
            getContext().become(this.waitingForFilepickerDocument(parent, msg.document, ws, firebaseSecret, filepickerSecret));
        } else {
            this.self().tell(new ReportGeneratorActorProtocol.RecievedError("bad filename"), this.self());
        }
    }

    private void processReceivedFile(ReportGeneratorActorProtocol.RecievedFile msg, ActorRef parent, WSClient ws, String firebaseSecret, String filepickerSecret) {
        if (msg.file.length < 1) {
            terminateWithFailureResponse(parent, "received 0 bytes from file storage");
        } else {
            ws.url("https://www.filepicker.io/api/store/S3")
                    .setQueryParameter("base64decode", "false")
                    .setQueryParameter("filename", "Отчет " + df.format(new Date()) + ".txt")
                    .setQueryParameter("key", filepickerSecret)
                    .post("123456789");
            this.terminateWithSuccessResponse(parent);
        }
    }

    private void terminateWithSuccessResponse(ActorRef parent) {
        parent.tell(new ReportGeneratorActorProtocol.GenerationSucceeded(), this.self());
        this.getContext().stop(this.self());
    }

    private void terminateWithFailureResponse(ActorRef parent, String msg) {
        parent.tell(new ReportGeneratorActorProtocol.GenerationFailure(msg), this.self());
        this.getContext().stop(this.self());
    }

    private void terminateSilently() {
        this.getContext().stop(this.self());
    }

    public static class TableMapping {

        public final int indexInReport;

        public final String searchInPlan;

        public final String header;

        private final BiConsumer<XWPFTable, XWPFTable> _map;

        public TableMapping(int indexInReport, String searchInPlan, String header, BiConsumer<XWPFTable, XWPFTable> map) {
            this.indexInReport = indexInReport;
            this.searchInPlan = searchInPlan;
            this.header = header;
            this._map = map;
        }

        public TableCreator map(XWPFTable table) {
            try {
                return new TableCreator(this, table);
            } catch (Exception e) {
                return null;
            }
        }

        public static class TableCreator {
            public final TableMapping mapper;
            public final XWPFTable table;

            public TableCreator(TableMapping mapper, XWPFTable table) {
                this.mapper = mapper;
                this.table = table;
            }

            public void create(XWPFDocument document) {
                XWPFTable newtable = document.createTable();
                mapper._map.accept(table, newtable);
            }
        }
    }

    private ImmutableList<TableMapping> getMappings() {
        List<TableMapping> result = new ArrayList<>();

        // Таблица 2 – План работы научно-методического семинара «Информационно-измерительные и управляющие системы»	Таблица 1 – Сведения о работе научно-методического семинара «Информационно-измерительные и управляющие системы»
        result.add(new TableMapping(
                1,
                "Таблица 2",
                "Таблица 1 – Сведения о работе научно-методического семинара «Информационно-измерительные и управляющие системы»",
                (table, newtable) -> {
                    List<XWPFTableRow> rows = table.getRows();

                    // создаем первую строку
                    XWPFTableRow tableRowOne = newtable.getRow(0);
                    tableRowOne.getCell(0).setText("№ п/п");
                    tableRowOne.addNewTableCell().setText("Дата проведения");
                    tableRowOne.addNewTableCell().setText("Тема семинара");
                    tableRowOne.addNewTableCell().setText("Рассмотренные вопросы");
                    tableRowOne.addNewTableCell().setText("Докладчики");

                    //create second row
                    XWPFTableRow tableRowTwo = newtable.createRow();
                    tableRowTwo.getCell(0).setText("1");
                    tableRowTwo.getCell(1).setText("2");
                    tableRowTwo.getCell(2).setText("3");
                    tableRowTwo.getCell(3).setText("4");
                    tableRowTwo.getCell(4).setText("5");

                    for (int i = 1; i<rows.size(); ++i) {
                        XWPFTableRow currentrow = table.getRow(i);
                        XWPFTableRow newrow = newtable.createRow();

                        moveText(currentrow, 0, newrow, 0);
                        moveText(currentrow, 1, newrow, 1);
                        moveText(currentrow, 2, newrow, 2);
                        newrow.getCell(3).setText("");
                        moveText(currentrow, 3, newrow, 4);
                    }
                }
        ));
        return new ImmutableList.Builder<TableMapping>()
                .addAll(result)
                .build();
    }

    private void moveText(XWPFTableRow oldrow, int oldindex, XWPFTableRow newrow, int newindex) {
        XWPFTableCell newcell = newrow.getCell(newindex);
        if (newcell != null) {
            XWPFTableCell oldcell = oldrow.getCell(oldindex);
            if (oldcell != null) {
                newcell.removeParagraph(0);
                oldcell.getParagraphs().forEach(p -> newcell.addParagraph(p));
            } else {
                newcell.setText("");
            }
        }
    }
}