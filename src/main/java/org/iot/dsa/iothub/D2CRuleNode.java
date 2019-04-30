package org.iot.dsa.iothub;

import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.restadapter.AbstractRuleNode;
import org.iot.dsa.dslink.restadapter.Constants;
import org.iot.dsa.dslink.restadapter.ResponseWrapper;
import org.iot.dsa.dslink.restadapter.WebClientProxy;
import org.iot.dsa.node.DSDouble;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSInt;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.time.DSDateTime;

public class D2CRuleNode extends AbstractRuleNode {
    
    private DSMap parameters;
    private D2CRule rule;
    
    private DSInfo lastRespCode = getInfo(Constants.LAST_RESPONSE_CODE);
    private DSInfo lastRespData = getInfo(Constants.LAST_RESPONSE_DATA);
    private DSInfo lastRespTs = getInfo(Constants.LAST_RESPONSE_TS);
    
    public D2CRuleNode() {
    }
    
    public D2CRuleNode(DSMap parameters) {
        this.parameters = parameters;
    }
    
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault(Constants.LAST_RESPONSE_CODE, DSInt.NULL).setReadOnly(true);
        declareDefault(Constants.LAST_RESPONSE_DATA, DSString.EMPTY).setReadOnly(true);
        declareDefault(Constants.LAST_RESPONSE_TS, DSString.EMPTY).setReadOnly(true);
    }
    
    @Override
    protected void onStarted() {
        super.onStarted();
        if (this.parameters == null) {
            DSIObject o = get(Constants.PARAMS);
            if (o instanceof DSMap) {
                this.parameters = (DSMap) o;
            }
        } else {
            put(Constants.PARAMS, parameters.copy()).setPrivate(true);
        }
    }
    
    @Override
    protected void onStable() {
        super.onStable();
        rule = new D2CRule(this, getSubscribePath(), getMessageProperties(), getBody(), getMinRefreshRate(), getMaxRefreshRate(), 0);
        put(Constants.ACT_EDIT, makeEditAction()).setTransient(true);
    }
    
    @Override
    protected void onRemoved() {
        if (rule != null) {
            rule.close();
        }
    }
    
    private DSIObject makeEditAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((D2CRuleNode) target.get()).edit(invocation.getParameters());
                return null;
            }
        };
        act.addDefaultParameter(Constants.SUB_PATH, DSString.valueOf(getSubscribePath()), null);
        act.addDefaultParameter("Properties", getMessageProperties().copy(), null);
        act.addDefaultParameter(Constants.REQUEST_BODY, DSString.valueOf(getBody()), null);
        act.addDefaultParameter(Constants.MIN_REFRESH_RATE, DSDouble.valueOf(getMinRefreshRate()), null);
        act.addDefaultParameter(Constants.MAX_REFRESH_RATE, DSDouble.valueOf(getMaxRefreshRate()), null);
        return act;
    }

    protected void edit(DSMap parameters) {
        for (Entry entry : parameters) {
            this.parameters.put(entry.getKey(), entry.getValue().copy());
        }
        put(Constants.PARAMS, parameters.copy());
        if (rule != null) {
            rule.close();
        }
        onStable();
    }
    
    public String getSubscribePath() {
        return parameters.getString(Constants.SUB_PATH);
    }
    
    public DSMap getMessageProperties() {
        return parameters.getMap("Properties");
    }
    
    public String getBody() {
        return parameters.getString(Constants.REQUEST_BODY);
    }
    
    public double getMinRefreshRate() {
        return parameters.get(Constants.MIN_REFRESH_RATE, 0.0);
    }
    
    public double getMaxRefreshRate() {
        return parameters.get(Constants.MAX_REFRESH_RATE, 0.0);
    }
    
    @Override
    public void responseRecieved(ResponseWrapper resp, int rowNum) {
        if (resp == null) {
            put(lastRespCode, DSInt.valueOf(-1));
            put(lastRespData, DSString.valueOf("Failed to send update"));
            put(lastRespTs, DSString.valueOf(DSDateTime.currentTime()));
        } else {
            int status = resp.getCode();
            String data = resp.getData();
            put(lastRespCode, DSInt.valueOf(status));
            put(lastRespData, DSString.valueOf(data));
            put(lastRespTs, DSString.valueOf(resp.getTS()));
        }
    }
    
    @Override
    public WebClientProxy getWebClientProxy() {
        return null;
    }
    
    @Override
    public DSIRequester getRequester() {
        return MainNode.getRequester();
    }

}
