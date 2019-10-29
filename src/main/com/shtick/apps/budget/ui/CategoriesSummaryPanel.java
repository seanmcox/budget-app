/**
 * 
 */
package com.shtick.apps.budget.ui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.shtick.apps.budget.GUIDriver;
import com.shtick.apps.budget.structure.model.Category;
import com.shtick.apps.budget.structure.model.CategoryID;

/**
 * @author scox
 *
 */
public class CategoriesSummaryPanel extends JPanel {

	/**
	 * 
	 */
	public CategoriesSummaryPanel() {
		super(new BorderLayout());
		JTree tree = new JTree(new CategoryTreeModel());
		tree.setRootVisible(false);
		JScrollPane scrollPane = new JScrollPane(tree);
		add(scrollPane, BorderLayout.BEFORE_LINE_BEGINS);
	}

	private class CategoryTreeModel implements TreeModel {
		private CategoryTreeNode root = new CategoryTreeNode(null);

		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeModel#getRoot()
		 */
		@Override
		public Object getRoot() {
			return root;
		}

		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
		 */
		@Override
		public Object getChild(Object parent, int index) {
			try {
				return ((CategoryTreeNode)parent).getChild(index);
			}
			catch(IOException t) {
				t.printStackTrace();
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
		 */
		@Override
		public int getChildCount(Object parent) {
			try {
				return ((CategoryTreeNode)parent).getChildCount();
			}
			catch(IOException t) {
				t.printStackTrace();
				return 0;
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
		 */
		@Override
		public boolean isLeaf(Object node) {
			return false;
		}

		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
		 */
		@Override
		public void valueForPathChanged(TreePath path, Object newValue) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int getIndexOfChild(Object parent, Object child) {
			try {
				if(!(child instanceof CategoryTreeNode))
					return -1;
				Category category = ((CategoryTreeNode)child).getCategory();
				if(category==null)
					return -1;
				return ((CategoryTreeNode)parent).getIndexOfChild(category.getId());
			}
			catch(IOException t) {
				t.printStackTrace();
				return -1;
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
		 */
		@Override
		public void addTreeModelListener(TreeModelListener l) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
		 */
		@Override
		public void removeTreeModelListener(TreeModelListener l) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class CategoryTreeNode{
		private Category category;
		private List<CategoryTreeNode> childNodes; 
		
		/**
		 * 
		 * @param category
		 */
		public CategoryTreeNode(Category category){
			this.category = category;
		}
		
		/**
		 * 
		 * @param index
		 * @return
		 * @throws IOException
		 */
		public CategoryTreeNode getChild(int index) throws IOException{
			populateChildNodesIfNotExist();
			return childNodes.get(index);
		}
		
		public int getChildCount() throws IOException{
			populateChildNodesIfNotExist();
			return childNodes.size();
		}

		public int getIndexOfChild(CategoryID categoryID) throws IOException{
			populateChildNodesIfNotExist();
			synchronized(this) {
				for(int i=0;i<childNodes.size();i++) {
					Category category = childNodes.get(i).category;
					if(category==null)
						continue;
					if(childNodes.get(i).category.getId().equals(categoryID))
						return i;
				}
			}
			return -1;
		}

		public Category getCategory() {
			return category;
		}
		
		public String toString() {
			if(category==null)
				return "[ROOT]";
			return category.getName();
		}
		
		private void populateChildNodesIfNotExist() throws IOException{
			synchronized(this) {
				if(childNodes == null) {
					childNodes = new LinkedList<>();
					List<Category> categories = GUIDriver.getGUIDriver().getCategories((category==null)?null:category.getId(), false);
					for(Category category:categories) {
						childNodes.add(new CategoryTreeNode(category));
					}
				}
			}
		}
	}
}
