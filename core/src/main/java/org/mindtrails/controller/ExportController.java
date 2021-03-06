package org.mindtrails.controller;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.joda.time.DateTime;
import org.joda.time.Years;
import org.mindtrails.domain.Conditions.ConditionAssignment;
import org.mindtrails.domain.ExportMode;
import org.mindtrails.domain.ImportMode;
import org.mindtrails.domain.Participant;
import org.mindtrails.domain.data.DoNotDelete;
import org.mindtrails.domain.RestExceptions.NoSuchIdException;
import org.mindtrails.domain.RestExceptions.NoSuchQuestionnaireException;
import org.mindtrails.domain.RestExceptions.NotDeleteableException;
import org.mindtrails.domain.importData.Scale;
import org.mindtrails.domain.tracking.ExportLog;
import org.mindtrails.persistence.*;
import org.mindtrails.service.ExportService;
import org.mindtrails.domain.piPlayer.Trial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Provides a tool for Exporting data from the system and then
 * safely removing that data once it is on the remote system.
 *
 */
@Controller
@RequestMapping("/api/export")
public class ExportController  {

    private static final Logger LOG = LoggerFactory.getLogger(ExportController.class);

    @Autowired
    ParticipantRepository participantRepository;
    @Autowired
    ExportLogRepository exportLogRepository;
    @Autowired ExportService exportService;



    @RequestMapping(method= RequestMethod.GET)
    public @ResponseBody List<Scale> list() {
        List<Scale> infoList = exportService.listRepositories();
        int sum = 0;
        for(Scale i : infoList) sum += i.getSize();
        exportLogRepository.save(new ExportLog(sum));
        return (infoList);
    }

    @RequestMapping(value="{name}", method= RequestMethod.GET)
    public @ResponseBody List<Object> listData
                (@PathVariable String name,
                 @RequestParam(value = "after", required = false, defaultValue = "1972-10-05-00-00-00")
                 @DateTimeFormat(pattern=ExportService.DATE_FORMAT)Date date) {
        JpaRepository rep = exportService.getRepositoryForName(name, true);
        List<Object> results;
        if (rep != null) {
            if (rep instanceof TrialRepository) {
                results = getTrialSummary((TrialRepository) rep);
            } else if(rep instanceof TaskLogRepository) {
                results = ((TaskLogRepository) rep).findByDateCompletedGreaterThan(date);
            } else if(rep instanceof ActionLogRepository) {
                results = ((ActionLogRepository) rep).findByDateGreaterThan(date);
            } else if(rep instanceof LogRepository) {
                results = ((LogRepository) rep).findByDateSentGreaterThan(date);
            } else if(rep instanceof QuestionnaireRepository) {
                results = ((QuestionnaireRepository) rep).findByDateGreaterThan(date);
            } else {
                results = rep.findAll();
            }
            return results;
        }
        throw new NoSuchQuestionnaireException();
    }

    @ExportMode
    @RequestMapping(value="{name}/{id}", method=RequestMethod.DELETE)
    public @ResponseBody void delete(@PathVariable String name, @PathVariable long id) {
        Class<?> domainType = exportService.getDomainType(name, false);
        LOG.info("Deleting " + name  + " / " + id);
        if (domainType != null) {
            if (domainType.isAnnotationPresent(DoNotDelete.class))
                throw new NotDeleteableException();
            JpaRepository rep = exportService.getRepositoryForName(name, true);
            try {
                rep.delete(id);
                rep.flush();
            } catch (EmptyResultDataAccessException e) {
                throw new NoSuchIdException();
            }
        } else {
            throw new NoSuchQuestionnaireException();
        }
    }

    /**
     * Return CSV
     */

//    public void writeCSVToResponse(PrintWriter writer, List<Object> object, String name)  {
//        try {
//            ColumnPositionMappingStrategy mapStrategy
//                    = new ColumnPositionMappingStrategy();
//            Class<?> domainType = exportService.getDomainType(name);
//            mapStrategy.setType(domainType);
//            mapStrategy.generateHeader();
//            //Field[] fields = object.get(0).getClass().getDeclaredFields();
//            Field[] fields = org.apache.commons.lang3.reflect.FieldUtils.getAllFields(domainType);
//            String[] columns = new String[fields.length];
//            int i=0;
//            for (Field cNames : fields) {
//                if (!cNames.getName().equals("participant")) {
//                columns[i] = cNames.getName();
//                i+=1;}
//            }
//            String joinedColumns = String.join("\t", columns);
//            writer.print(joinedColumns+"\n");
//            mapStrategy.setColumnMapping(columns);
//            StatefulBeanToCsv btcsv = new StatefulBeanToCsvBuilder(writer)
//                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
//                    .withMappingStrategy(mapStrategy)
//                    .withSeparator('\t')
//                    .build();
//            btcsv.write(object);
//        } catch (CsvException ex) {
//            LOG.error("Error mapping Bean to CSV", ex);
//        }
//    }

    @ImportMode
    @RequestMapping(value = "{name}.csv", method= RequestMethod.GET)
    public void listAsCSV(HttpServletResponse response, @PathVariable String name) throws IOException {

        List<Object> json = listData(name, DateTime.now().minus(Years.years(5)).toDate());
        response.setContentType("text/plain; charset=utf-8");
        Class<?> domainType = exportService.getDomainType(name, true);
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(domainType).withHeader();
        schema = schema.withColumnSeparator('\t');
        ObjectWriter myObjectWriter = mapper.writer(schema);
        if (json != null) {
//            writeCSVToResponse(response.getWriter(), json, name);
            myObjectWriter.writeValue(response.getWriter(),json);

        } else {
            response.getWriter().print("Empty dataset.");
        }
    }

    /**
     * Returns the json data of a PIPlayer script, removing non-essential data so
     * the exporter can more easily handle it.
     * @return
     */
    public List<Object> getTrialSummary(TrialRepository trialRepository) {
        List<Trial> trialData = trialRepository.findAll();
        List<Object> reports = new ArrayList<>();
        // Convert to trial summary
        for (Trial data : trialData) {
            reports.add(data.toTrialJson().toInterpretationReport());
        }
        return reports;
    }


    /** Update the control condition to which a participant is assigned.
     *  useful if the information needed to assign a participant to a
     *  control condition only resides on the backend/private/import side.
     */
    @ExportMode
    @RequestMapping(value = "condition", method= RequestMethod.POST)
    public @ResponseBody ConditionAssignment assignCondition(@RequestBody ConditionAssignment assignment) {
        Participant participant = participantRepository.findOne(assignment.getParticipantId());
        if(participant != null) {
            participant.getStudy().setConditioning(assignment.getCondition());
            participantRepository.save(participant);
            participantRepository.flush();
            return assignment;
        } else {
            throw new NoSuchIdException();
        }
    }


}


