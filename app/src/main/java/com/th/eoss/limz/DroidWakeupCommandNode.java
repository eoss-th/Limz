package com.th.eoss.limz;

import android.content.Context;

import com.eoss.brain.MessageObject;
import com.eoss.brain.Session;
import com.eoss.brain.command.CommandNode;
import com.eoss.brain.command.ForwardCommandNode;
import com.eoss.brain.command.talk.FeedbackCommandNode;

import java.util.Arrays;
import java.util.List;

/**
 * Created by eossth on 7/31/2017 AD.
 */
public class DroidWakeupCommandNode extends CommandNode {

    Context androidContext;

    public DroidWakeupCommandNode(Session session, Context androidContext) {
        super(session, null);
        this.androidContext = androidContext;
    }

    @Override
    public String execute(MessageObject messageObject) {

        try {
            session.context.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        session.commandList.clear();

        List<String> rejectKeys = Arrays.asList(androidContext.getResources().getString(R.string.no), androidContext.getResources().getString(R.string.ok), androidContext.getResources().getString(R.string.no), androidContext.getResources().getString(R.string.ok));

        //Feedback for learning mode
        session.commandList.add(new FeedbackCommandNode(session, new String[]{androidContext.getResources().getString(R.string.no)}, "?", -0.1f, rejectKeys));

        //Positive Feedback
        session.commandList.add(new FeedbackCommandNode(session, new String[]{androidContext.getResources().getString(R.string.yes)}, androidContext.getResources().getString(R.string.thankyou), 0.1f));

        session.commandList.add(new ForwardCommandNode(session, new String[]{androidContext.getResources().getString(R.string.next)}));

        List<String> lowConfidenceKeys = Arrays.asList(androidContext.getResources().getString(R.string.ok), androidContext.getResources().getString(R.string.no), androidContext.getResources().getString(R.string.ok), "?");

        session.commandList.add(new DroidTalkCommandNode(session, lowConfidenceKeys));

        return "";
    }
}
