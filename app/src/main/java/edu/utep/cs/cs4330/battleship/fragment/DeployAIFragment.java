// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.fragment;

import android.os.Bundle;
//import android.support.annotation.IdRes;
//import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import edu.utep.cs.cs4330.battleship.R;
import edu.utep.cs.cs4330.battleship.activity.DeploymentActivity;
import edu.utep.cs.cs4330.battleship.ai.StrategyType;

public class DeployAIFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {
    private DeploymentActivity activity;
    private RadioGroup radioGroupStrategy;
    public StrategyType strategyAI;

    public DeployAIFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (DeploymentActivity) getActivity();

        View view = inflater.inflate(R.layout.fragment_deploy_ai, container, false);
        radioGroupStrategy = (RadioGroup) view.findViewById(R.id.radioGroupStrategy);
        radioGroupStrategy.setOnCheckedChangeListener(this);

        return view;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        if (checkedId == R.id.radioRandom)
            strategyAI = StrategyType.Random;
        else if (checkedId == R.id.radioSweep)
            strategyAI = StrategyType.Sweep;

        activity.onFragmentUpdate(true);
    }
}
